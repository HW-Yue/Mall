package com.yue.common.log.webflux;

import com.yue.common.log.conditional.OnSkyWalkingTraceModeCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;

/**
 * SkyWalking 模式：注册 Reactor MDC 全局 Hook（与 legacy 的 {@link TraceIdWebFluxConfiguration} 相同需求）。
 * <p>Spring Cloud Gateway 专用 GlobalFilter 见 {@link SkyWalkingGatewayFilterConfiguration}。
 */
@Configuration
@ConditionalOnClass({Mono.class, Flux.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@Conditional(OnSkyWalkingTraceModeCondition.class)
public class SkyWalkingWebFluxConfiguration {

    @PostConstruct
    public void init() {
        TraceIdContextLifter.registerHook();
    }
}
