package com.yue.groupbuy.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Agent 通过 Nacos 热更新的业务侧运行时开关（缓存策略、非核心功能）。
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.agent")
public class AgentRuntimeProperties {

    private Cache cache = new Cache();
    private Features features = new Features();

    @Data
    public static class Cache {
        private long redisDefaultTtlSeconds = 3600;
        private long caffeineExpireAfterWriteSeconds = 300;
        /** CACHE_FIRST：先缓存；DIRECT_DB：降级直读库；BACKUP_CACHE：读备份/从缓存。 */
        private String cacheFallbackStrategy = "CACHE_FIRST";
    }

    @Data
    public static class Features {
        private boolean marketingPushEnabled = true;
        private boolean statisticsReportEnabled = true;
    }
}
