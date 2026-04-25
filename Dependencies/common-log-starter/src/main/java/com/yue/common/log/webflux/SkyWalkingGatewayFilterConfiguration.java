package com.yue.common.log.webflux;

import com.yue.common.log.CommonLogProperties;
import com.yue.common.log.conditional.OnSkyWalkingTraceModeCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * 仅在类路径含 Spring Cloud Gateway 时注册；将 SkyWalking trace id 放入 Reactor Context（见 {@link SkyWalkingGatewayMdcFilter}）。
 */
@Configuration
@ConditionalOnClass(GlobalFilter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@Conditional(OnSkyWalkingTraceModeCondition.class)
public class SkyWalkingGatewayFilterConfiguration {

    @Bean
    public GlobalFilter skyWalkingGatewayMdcFilter(CommonLogProperties properties) {
        return new SkyWalkingGatewayMdcFilter(properties);
    }
}
