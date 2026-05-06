package com.yue.groupbuy.config;

import com.yue.groupbuy.infrastructure.config.AgentRuntimeProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRuntimePropertiesTest {

    @Test
    void shouldBindInitialAndUpdatedRuntimeProperties() {
        AgentRuntimeProperties before = bind(Map.of(
                "app.agent.cache.redis-default-ttl-seconds", "3600",
                "app.agent.cache.caffeine-expire-after-write-seconds", "300",
                "app.agent.cache.cache-fallback-strategy", "CACHE_FIRST",
                "app.agent.features.marketing-push-enabled", "true",
                "app.agent.features.statistics-report-enabled", "true",
                "app.agent.feign.order-service.connect-timeout-ms", "5000",
                "app.agent.feign.order-service.read-timeout-ms", "10000"
        ));
        printState("before-refresh", before);

        AgentRuntimeProperties after = bind(Map.of(
                "app.agent.cache.redis-default-ttl-seconds", "1800",
                "app.agent.cache.caffeine-expire-after-write-seconds", "120",
                "app.agent.cache.cache-fallback-strategy", "DIRECT_DB",
                "app.agent.features.marketing-push-enabled", "false",
                "app.agent.features.statistics-report-enabled", "false",
                "app.agent.feign.order-service.connect-timeout-ms", "2500",
                "app.agent.feign.order-service.read-timeout-ms", "4000"
        ));
        printState("after-refresh", after);

        assertThat(before.getCache().getRedisDefaultTtlSeconds()).isEqualTo(3600);
        assertThat(before.getCache().getCaffeineExpireAfterWriteSeconds()).isEqualTo(300);
        assertThat(before.getCache().getCacheFallbackStrategy()).isEqualTo("CACHE_FIRST");
        assertThat(before.getFeatures().isMarketingPushEnabled()).isTrue();
        assertThat(before.getFeatures().isStatisticsReportEnabled()).isTrue();
        assertThat(before.getFeign().getOrderService().getConnectTimeoutMs()).isEqualTo(5000);
        assertThat(before.getFeign().getOrderService().getReadTimeoutMs()).isEqualTo(10000);

        assertThat(after.getCache().getRedisDefaultTtlSeconds()).isEqualTo(1800);
        assertThat(after.getCache().getCaffeineExpireAfterWriteSeconds()).isEqualTo(120);
        assertThat(after.getCache().getCacheFallbackStrategy()).isEqualTo("DIRECT_DB");
        assertThat(after.getFeatures().isMarketingPushEnabled()).isFalse();
        assertThat(after.getFeatures().isStatisticsReportEnabled()).isFalse();
        assertThat(after.getFeign().getOrderService().getConnectTimeoutMs()).isEqualTo(2500);
        assertThat(after.getFeign().getOrderService().getReadTimeoutMs()).isEqualTo(4000);
    }

    private static AgentRuntimeProperties bind(Map<String, Object> values) {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", values));
        return Binder.get(environment)
                .bind("app.agent", Bindable.of(AgentRuntimeProperties.class))
                .orElseThrow(IllegalStateException::new);
    }

    private static void printState(String phase, AgentRuntimeProperties properties) {
        System.out.printf(
                "[AgentRuntimePropertiesTest-group-buy] phase=%s redisTtl=%d caffeineTtl=%d fallback=%s push=%s stats=%s connect=%d read=%d%n",
                phase,
                properties.getCache().getRedisDefaultTtlSeconds(),
                properties.getCache().getCaffeineExpireAfterWriteSeconds(),
                properties.getCache().getCacheFallbackStrategy(),
                properties.getFeatures().isMarketingPushEnabled(),
                properties.getFeatures().isStatisticsReportEnabled(),
                properties.getFeign().getOrderService().getConnectTimeoutMs(),
                properties.getFeign().getOrderService().getReadTimeoutMs());
    }
}
