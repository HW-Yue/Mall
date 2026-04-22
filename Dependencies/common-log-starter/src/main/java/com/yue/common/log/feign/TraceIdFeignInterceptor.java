package com.yue.common.log.feign;

import com.yue.common.log.CommonLogConstants;
import com.yue.common.log.TraceIdContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * Feign 调用 TraceId 透传拦截器
 *
 * <p>在通过 Feign 调用下游服务时，自动从当前线程 MDC 中获取 trace-id，
 * 并添加到 HTTP Header 中，实现全链路追踪。
 *
 * <p>使用方式：
 * <pre>
 * @FeignClient(name = "order-service", configuration = TraceIdFeignConfiguration.class)
 * public interface OrderServiceClient { ... }
 * </pre>
 *
 * <p>或在全局配置中注册（推荐，由自动配置完成）。
 */
@Slf4j
public class TraceIdFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String traceId = TraceIdContext.get();

        if (traceId != null && !traceId.isEmpty()) {
            template.header(CommonLogConstants.HEADER_NAME, traceId);

            if (log.isDebugEnabled()) {
                log.debug("[TraceId] Feign 请求透传 trace-id: {} -> {}", traceId, template.url());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("[TraceId] Feign 请求未找到 trace-id，跳过透传: {}", template.url());
            }
        }
    }

}
