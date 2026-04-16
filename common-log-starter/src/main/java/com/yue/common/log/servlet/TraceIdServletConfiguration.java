package com.yue.common.log.servlet;

import com.yue.common.log.CommonLogProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.Filter;

/**
 * Servlet 环境 TraceId 自动配置
 *
 * <p>条件：
 * <ul>
 *     <li>类路径存在 javax.servlet.Filter</li>
 *     <li>是 Servlet Web 应用</li>
 *     <li>yue.log.trace.enabled = true（默认）</li>
 * </ul>
 */
@Configuration
@ConditionalOnClass(Filter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "yue.log.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TraceIdServletConfiguration {

    /**
     * 注册 TraceId 过滤器
     * 优先级：Ordered.HIGHEST_PRECEDENCE + 100，确保在其他过滤器之前执行
     */
    @Bean
    public FilterRegistrationBean<TraceIdServletFilter> traceIdServletFilterRegistration(CommonLogProperties properties) {
        FilterRegistrationBean<TraceIdServletFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdServletFilter(properties));
        registration.addUrlPatterns("/*");
        registration.setName("traceIdServletFilter");
        registration.setOrder(-100); // 高优先级，确保最先执行
        return registration;
    }

}
