package com.yue.common.log.feign;

import feign.Feign;
import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign TraceId 自动配置
 *
 * <p>条件：
 * <ul>
 *     <li>类路径存在 feign.Feign</li>
 *     <li>yue.log.trace.enabled = true（默认）</li>
 * </ul>
 *
 * <p>注册后，所有 Feign 客户端都会自动带上 trace-id 请求头。
 */
@Configuration
@ConditionalOnClass(Feign.class)
@ConditionalOnProperty(prefix = "yue.log.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TraceIdFeignConfiguration {

    /**
     * 注册 Feign 拦截器，自动透传 trace-id
     */
    @Bean
    public RequestInterceptor traceIdFeignInterceptor() {
        return new TraceIdFeignInterceptor();
    }

}
