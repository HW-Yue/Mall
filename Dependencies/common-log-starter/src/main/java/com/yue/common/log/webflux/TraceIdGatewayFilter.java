package com.yue.common.log.webflux;

import com.yue.common.log.CommonLogConstants;
import com.yue.common.log.CommonLogProperties;
import com.yue.common.log.TraceIdContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * 网关层 TraceId 全局过滤器
 *
 * <p>职责：
 * <ol>
 *     <li>从 HTTP Header 获取 trace-id，如果没有则生成新的</li>
 *     <li>将 trace-id 放入 Reactor Context（用于 WebFlux 线程切换）</li>
 *     <li>将 trace-id 添加到转发的下游请求 Header 中</li>
 *     <li>可选：在 HTTP 响应头中返回 trace-id</li>
 * </ol>
 */
@Slf4j
public class TraceIdGatewayFilter implements GlobalFilter, Ordered {

    private final CommonLogProperties properties;

    public TraceIdGatewayFilter(CommonLogProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. 从 Header 获取或生成 trace-id
        String traceId = request.getHeaders().getFirst(CommonLogConstants.HEADER_NAME);
        if (traceId == null || traceId.isEmpty()) {
            traceId = TraceIdContext.generate();
            if (log.isDebugEnabled()) {
                log.debug("[TraceId] 网关生成新 trace-id: {}", traceId);
            }
        }

        final String finalTraceId = traceId;

        // 2. 将 trace-id 添加到转发的请求头中
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CommonLogConstants.HEADER_NAME, finalTraceId)
                .build();

        // 3. 如果需要，在响应头中返回 trace-id
        ServerHttpResponse response = exchange.getResponse();
        if (properties.isResponseHeader()) {
            response.getHeaders().add(CommonLogConstants.HEADER_NAME, finalTraceId);
        }

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .response(response)
                .build();

        // 4. 继续过滤器链，并将 trace-id 放入 Reactor Context
        return chain.filter(mutatedExchange)
                .contextWrite(Context.of(TraceIdContextLifter.TRACE_ID_KEY, finalTraceId));
    }

    @Override
    public int getOrder() {
        // 最高优先级，确保在其他过滤器之前执行
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

}
