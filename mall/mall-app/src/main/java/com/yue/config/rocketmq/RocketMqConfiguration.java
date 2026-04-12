package com.yue.config.rocketmq;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用 RocketMQ 业务配置项；仅当配置了 {@code rocketmq.name-server} 时注册，避免未部署 RocketMQ 时强行拉取客户端。
 */
@Configuration
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@EnableConfigurationProperties(RocketMqTopicProperties.class)
public class RocketMqConfiguration {
}
