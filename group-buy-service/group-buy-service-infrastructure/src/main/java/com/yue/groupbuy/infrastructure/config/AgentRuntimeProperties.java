package com.yue.groupbuy.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Agent 通过 Nacos 热更新的业务侧运行时开关（缓存策略、非核心功能、Feign 超时等）。
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.agent")
public class AgentRuntimeProperties {

    private Cache cache = new Cache();
    private Features features = new Features();
    private Feign feign = new Feign();

    @Data
    public static class Cache {
        /**
         * Redis 侧默认 TTL（秒），业务在封装缓存写入时使用该值。
         */
        private long redisDefaultTtlSeconds = 3600;
        /**
         * 本地 Caffeine 过期（秒），与 spring.cache.caffeine.spec 配合或自行建 Cache 时使用。
         */
        private long caffeineExpireAfterWriteSeconds = 300;
        /**
         * CACHE_FIRST：先缓存；DIRECT_DB：降级直读库；BACKUP_CACHE：读备份/从缓存。
         */
        private String cacheFallbackStrategy = "CACHE_FIRST";
    }

    @Data
    public static class Features {
        private boolean marketingPushEnabled = true;
        private boolean statisticsReportEnabled = true;
    }

    @Data
    public static class Feign {
        private OrderService orderService = new OrderService();

        @Data
        public static class OrderService {
            private int connectTimeoutMs = 5000;
            private int readTimeoutMs = 10000;
        }
    }
}
