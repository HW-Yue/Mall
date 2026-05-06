package com.yue.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RocketMQ 消费线程池动态配置，按 consumerGroup 维度覆盖。
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.rocketmq.consumer-thread-pool")
public class RocketMqConsumerThreadPoolProperties {

    private Map<String, Consumer> consumers = new LinkedHashMap<>();

    @Data
    public static class Consumer {
        private Integer consumeThreadNumber;
        private Integer consumeThreadMax;
    }
}
