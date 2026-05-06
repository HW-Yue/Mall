package cn.bugstack.config;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.impl.consumer.ConsumeMessageConcurrentlyService;
import org.apache.rocketmq.client.impl.consumer.DefaultMQPushConsumerImpl;
import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RocketMqConsumerThreadPoolRefresherTest {

    @Test
    void shouldUpdateRunningConsumerThreadPoolOnStartupAndRefresh() {
        String consumerGroup = "CG_PAY_ORDER_CLOSE_NORMAL";

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setConsumeThreadMin(10);
        consumer.setConsumeThreadMax(32);

        DefaultMQPushConsumerImpl consumerImpl = consumer.getDefaultMQPushConsumerImpl();
        MessageListenerConcurrently messageListener = (msgs, context) -> ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        ConsumeMessageConcurrentlyService consumeMessageService =
                new ConsumeMessageConcurrentlyService(consumerImpl, messageListener);
        setConsumeMessageService(consumerImpl, consumeMessageService);
        ThreadPoolExecutor consumeExecutor = extractConsumeExecutor(consumeMessageService);

        DefaultRocketMQListenerContainer container = new DefaultRocketMQListenerContainer();
        container.setConsumerGroup(consumerGroup);
        container.setConsumer(consumer);

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("orderCloseNormalListenerContainer", container);

        RocketMqConsumerThreadPoolProperties properties = new RocketMqConsumerThreadPoolProperties();
        properties.setConsumers(Map.of(consumerGroup, consumerConfig(20, 64)));

        RocketMqConsumerThreadPoolRefresher refresher =
                new RocketMqConsumerThreadPoolRefresher(beanFactory, properties);

        printState("before-startup-apply", consumer, consumeExecutor);
        refresher.afterSingletonsInstantiated();
        printState("after-startup-apply", consumer, consumeExecutor);

        assertEquals(20, consumer.getConsumeThreadMin());
        assertEquals(64, consumer.getConsumeThreadMax());
        assertEquals(20, consumeExecutor.getCorePoolSize());
        assertEquals(64, consumeExecutor.getMaximumPoolSize());

        properties.setConsumers(Map.of(consumerGroup, consumerConfig(8, 16)));

        printState("before-refresh-event", consumer, consumeExecutor);
        refresher.onApplicationEvent(new EnvironmentChangeEvent(this, Set.of(
                "app.rocketmq.consumer-thread-pool.consumers.CG_PAY_ORDER_CLOSE_NORMAL.consume-thread-number",
                "app.rocketmq.consumer-thread-pool.consumers.CG_PAY_ORDER_CLOSE_NORMAL.consume-thread-max"
        )));
        printState("after-refresh-event", consumer, consumeExecutor);

        assertEquals(8, consumer.getConsumeThreadMin());
        assertEquals(16, consumer.getConsumeThreadMax());
        assertEquals(8, consumeExecutor.getCorePoolSize());
        assertEquals(16, consumeExecutor.getMaximumPoolSize());
    }

    private static RocketMqConsumerThreadPoolProperties.Consumer consumerConfig(int min, int max) {
        RocketMqConsumerThreadPoolProperties.Consumer consumer = new RocketMqConsumerThreadPoolProperties.Consumer();
        consumer.setConsumeThreadNumber(min);
        consumer.setConsumeThreadMax(max);
        return consumer;
    }

    private static void setConsumeMessageService(DefaultMQPushConsumerImpl consumerImpl, Object consumeMessageService) {
        Field field = ReflectionUtils.findField(DefaultMQPushConsumerImpl.class, "consumeMessageService");
        if (field == null) {
            throw new IllegalStateException("consumeMessageService field not found");
        }
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, consumerImpl, consumeMessageService);
    }

    private static ThreadPoolExecutor extractConsumeExecutor(ConsumeMessageConcurrentlyService consumeMessageService) {
        Field field = ReflectionUtils.findField(ConsumeMessageConcurrentlyService.class, "consumeExecutor");
        if (field == null) {
            throw new IllegalStateException("consumeExecutor field not found");
        }
        ReflectionUtils.makeAccessible(field);
        return (ThreadPoolExecutor) ReflectionUtils.getField(field, consumeMessageService);
    }

    private static void printState(String phase, DefaultMQPushConsumer consumer, ThreadPoolExecutor executor) {
        System.out.printf(
                "[RocketMqConsumerThreadPoolRefresherTest-pay] phase=%s consumerMin=%d consumerMax=%d executorCore=%d executorMax=%d%n",
                phase,
                consumer.getConsumeThreadMin(),
                consumer.getConsumeThreadMax(),
                executor.getCorePoolSize(),
                executor.getMaximumPoolSize()
        );
    }
}
