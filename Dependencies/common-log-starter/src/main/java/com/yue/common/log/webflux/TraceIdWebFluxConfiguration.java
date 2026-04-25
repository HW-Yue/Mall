package com.yue.common.log.webflux;

import com.yue.common.log.CommonLogProperties;
import com.yue.common.log.conditional.OnLegacyTraceModeCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Conditional;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;

/**
 * WebFlux 环境 TraceId 自动配置
 *
 * <p>条件：
 * <ul>
 *     <li>类路径存在 reactor.core.publisher.Mono</li>
 *     <li>是 Reactive Web 应用</li>
 *     <li>yue.log.trace.enabled = true（默认）</li>
 * </ul>
 */
@Configuration
@ConditionalOnClass({Mono.class, Flux.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@Conditional(OnLegacyTraceModeCondition.class)
public class TraceIdWebFluxConfiguration {

    /**
     * 注册全局 Hook，实现 Reactor Context 与 MDC 的自动同步
     */
    @PostConstruct
    public void init() {
        TraceIdContextLifter.registerHook();
    }

    /**
     * 注册网关 TraceId 过滤器（如果是 Gateway）
     * 注意：如果类路径没有 GlobalFilter，则不会生效
     */
    @Bean
    @ConditionalOnClass(GlobalFilter.class)
    public TraceIdGatewayFilter traceIdGatewayFilter(CommonLogProperties properties) {
        return new TraceIdGatewayFilter(properties);
    }

}
