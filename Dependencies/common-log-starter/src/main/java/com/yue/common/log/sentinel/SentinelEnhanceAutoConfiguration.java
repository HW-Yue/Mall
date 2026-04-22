package com.yue.common.log.sentinel;

import com.alibaba.csp.sentinel.Sph;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Sentinel 共享增强自动配置。
 * <p>仅当业务服务引入了 Sentinel Starter 时才生效（通过 {@link ConditionalOnClass} 保证）。
 * <p>提供：
 * <ul>
 *     <li>统一 BlockExceptionHandler（MVC / Gateway 两种）</li>
 *     <li>Sentinel → Micrometer 指标桥接</li>
 * </ul>
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(Sph.class)
@ConditionalOnProperty(prefix = "yue.sentinel", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SentinelCommonProperties.class)
public class SentinelEnhanceAutoConfiguration {

    // ------------------------------------------------------------------
    // 1. Servlet 模式（业务服务）：注册 MVC 限流异常处理器
    // ------------------------------------------------------------------
    @AutoConfiguration
    @ConditionalOnClass(name = {
            "jakarta.servlet.http.HttpServletRequest",
            "com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler"
    })
    public static class ServletBlockHandlerConfig {

        @Bean
        @ConditionalOnMissingBean(com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler.class)
        public SentinelBlockExceptionHandler sentinelBlockExceptionHandler(SentinelCommonProperties properties) {
            log.info("[Sentinel] ServletBlockExceptionHandler registered, httpStatus={}, code={}",
                    properties.getBlockHttpStatus(), properties.getBlockCode());
            return new SentinelBlockExceptionHandler(properties);
        }
    }

    // ------------------------------------------------------------------
    // 2. Gateway 模式：注册 WebFlux 版本的 BlockRequestHandler
    // ------------------------------------------------------------------
    @AutoConfiguration
    @ConditionalOnClass(name = {
            "com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler"
    })
    public static class GatewayBlockHandlerConfig {

        @Bean
        @ConditionalOnMissingBean
        public SentinelGatewayBlockHandler sentinelGatewayBlockHandler(SentinelCommonProperties properties) {
            return new SentinelGatewayBlockHandler(properties);
        }
    }

    // ------------------------------------------------------------------
    // 3. Micrometer 指标桥接：暴露到 /actuator/prometheus
    // ------------------------------------------------------------------
    @AutoConfiguration
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnProperty(prefix = "yue.sentinel", name = "metrics-enabled", havingValue = "true", matchIfMissing = true)
    public static class MetricsBinderConfig {

        @Bean
        @ConditionalOnMissingBean
        public SentinelMetricsBinder sentinelMetricsBinder(SentinelCommonProperties properties,
                                                           @Value("${spring.application.name:application}") String applicationName) {
            // Spring Boot 会自动识别 MeterBinder 类型的 Bean 并调用 bindTo(MeterRegistry)
            return new SentinelMetricsBinder(properties, applicationName);
        }
    }
}
