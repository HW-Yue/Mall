package com.yue.test.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * test-mock 下的 RocketMQTemplate 替身，避免单测连真实 broker。
 */
@TestConfiguration
@Profile("test-mock")
public class RocketMqMockTestConfig {

    @Bean
    public RocketMQTemplate rocketMQTemplate() {
        return Mockito.mock(RocketMQTemplate.class);
    }
}
