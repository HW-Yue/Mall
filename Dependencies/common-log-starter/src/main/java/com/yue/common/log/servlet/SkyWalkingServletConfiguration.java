package com.yue.common.log.servlet;

import com.yue.common.log.CommonLogProperties;
import com.yue.common.log.conditional.OnSkyWalkingTraceModeCondition;
import com.yue.common.log.skywalking.SkyWalkingTraceMdcFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import jakarta.servlet.Filter;

/**
 * SkyWalking 模式：仅把 {@link org.apache.skywalking.apm.toolkit.trace.TraceContext} 写入 MDC，不自己生成/透传 trace-id。需配合 Java Agent 使用。
 */
@Configuration
@ConditionalOnClass(Filter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Conditional(OnSkyWalkingTraceModeCondition.class)
public class SkyWalkingServletConfiguration {

    @Bean
    public FilterRegistrationBean<SkyWalkingTraceMdcFilter> skyWalkingTraceMdcFilterRegistration(CommonLogProperties properties) {
        SkyWalkingTraceMdcFilter filter = new SkyWalkingTraceMdcFilter(properties);
        FilterRegistrationBean<SkyWalkingTraceMdcFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setName("skyWalkingTraceMdcFilter");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 20);
        return registration;
    }
}
