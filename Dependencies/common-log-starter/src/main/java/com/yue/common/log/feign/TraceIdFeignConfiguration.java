package com.yue.common.log.feign;

import com.yue.common.log.conditional.OnLegacyTraceModeCondition;
import feign.Feign;
import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
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
@Conditional(OnLegacyTraceModeCondition.class)
public class TraceIdFeignConfiguration {

    /**
     * 注册 Feign 拦截器，自动透传 trace-id
     */
    @Bean
    public RequestInterceptor traceIdFeignInterceptor() {
        return new TraceIdFeignInterceptor();
    }

}
