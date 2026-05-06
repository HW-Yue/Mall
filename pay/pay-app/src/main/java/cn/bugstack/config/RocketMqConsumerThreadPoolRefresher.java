package cn.bugstack.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Nacos 配置刷新后，按 consumerGroup 动态调整 RocketMQ 消费线程池。
 */
@Slf4j
@Component
public class RocketMqConsumerThreadPoolRefresher implements SmartInitializingSingleton,
        ApplicationListener<EnvironmentChangeEvent> {

    private static final String PREFIX = "app.rocketmq.consumer-thread-pool.";

    private final ListableBeanFactory beanFactory;
    private final RocketMqConsumerThreadPoolProperties properties;

    public RocketMqConsumerThreadPoolRefresher(ListableBeanFactory beanFactory,
                                               RocketMqConsumerThreadPoolProperties properties) {
        this.beanFactory = beanFactory;
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        applyAll("startup");
    }

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        boolean touched = event.getKeys().stream().anyMatch(k -> k.startsWith(PREFIX));
        if (touched) {
            applyAll("refresh");
        }
    }

    private void applyAll(String reason) {
        Map<String, DefaultRocketMQListenerContainer> containers =
                beanFactory.getBeansOfType(DefaultRocketMQListenerContainer.class);
        containers.forEach((beanName, container) -> apply(beanName, container, reason));
    }

    private void apply(String beanName, DefaultRocketMQListenerContainer container, String reason) {
        String consumerGroup = container.getConsumerGroup();
        RocketMqConsumerThreadPoolProperties.Consumer config = properties.getConsumers().get(consumerGroup);
        if (config == null) {
            return;
        }

        DefaultMQPushConsumer consumer = container.getConsumer();
        if (consumer == null) {
            log.warn("skip RocketMQ consumer thread refresh, consumer not ready. bean:{} group:{}", beanName, consumerGroup);
            return;
        }

        int targetMin = sanitize(config.getConsumeThreadNumber(), consumer.getConsumeThreadMin(), consumerGroup, "consumeThreadNumber");
        int targetMax = sanitize(config.getConsumeThreadMax(), consumer.getConsumeThreadMax(), consumerGroup, "consumeThreadMax");
        if (targetMin > targetMax) {
            log.warn("RocketMQ consumer thread config invalid, promote max to min. group:{} min:{} max:{}",
                    consumerGroup, targetMin, targetMax);
            targetMax = targetMin;
        }

        int currentMin = consumer.getConsumeThreadMin();
        int currentMax = consumer.getConsumeThreadMax();
        if (currentMin == targetMin && currentMax == targetMax) {
            return;
        }

        applyToRunningExecutor(consumer, targetMin, targetMax);
        consumer.setConsumeThreadMin(targetMin);
        consumer.setConsumeThreadMax(targetMax);

        log.info("RocketMQ consumer thread pool refreshed [{}]. bean:{} group:{} min:{}->{} max:{}->{}",
                reason, beanName, consumerGroup, currentMin, targetMin, currentMax, targetMax);
    }

    private void applyToRunningExecutor(DefaultMQPushConsumer consumer, int targetMin, int targetMax) {
        ThreadPoolExecutor executor = extractConsumeExecutor(consumer);
        if (executor == null) {
            return;
        }

        if (targetMax < executor.getCorePoolSize()) {
            executor.setCorePoolSize(targetMin);
            executor.setMaximumPoolSize(targetMax);
            return;
        }

        executor.setMaximumPoolSize(targetMax);
        executor.setCorePoolSize(targetMin);
    }

    private ThreadPoolExecutor extractConsumeExecutor(DefaultMQPushConsumer consumer) {
        Object consumeMessageService = consumer.getDefaultMQPushConsumerImpl().getConsumeMessageService();
        if (consumeMessageService == null) {
            return null;
        }

        Field field = ReflectionUtils.findField(consumeMessageService.getClass(), "consumeExecutor");
        if (field == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(field);
        Object executor = ReflectionUtils.getField(field, consumeMessageService);
        return executor instanceof ThreadPoolExecutor ? (ThreadPoolExecutor) executor : null;
    }

    private int sanitize(Integer configuredValue, int currentValue, String consumerGroup, String field) {
        if (configuredValue == null) {
            return currentValue;
        }
        int sanitized = Math.max(1, Math.min(1000, configuredValue));
        if (!Objects.equals(configuredValue, sanitized)) {
            log.warn("RocketMQ consumer thread config out of range, clamp to {}. group:{} field:{} raw:{}",
                    sanitized, consumerGroup, field, configuredValue);
        }
        return sanitized;
    }
}
