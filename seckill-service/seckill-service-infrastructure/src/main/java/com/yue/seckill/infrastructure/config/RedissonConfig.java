package com.yue.seckill.infrastructure.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端配置（用于分布式锁 + 看门狗）
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:${spring.redis.host:localhost}}")
    private String host;

    @Value("${spring.data.redis.port:${spring.redis.port:6379}}")
    private int port;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                // 看门狗续约间隔 = lockWatchdogTimeout / 3，默认 30s / 3 = 10s 续约一次
                .setConnectionPoolSize(64)
                .setConnectionMinimumIdleSize(8);
        return Redisson.create(config);
    }

}
