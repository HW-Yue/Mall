package cn.bugstack.test.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * test-mock 下的 RocketMQ 替身。
 * <p>
 * 只保留参数组装与方法调用路径，不触发真实 MQ 连接。
 * </p>
 */
@TestConfiguration
@Profile("test-mock")
public class RocketMqMockTestConfig {

    @Bean
    public RocketMQTemplate rocketMQTemplate() {
        return Mockito.mock(RocketMQTemplate.class);
    }
}
