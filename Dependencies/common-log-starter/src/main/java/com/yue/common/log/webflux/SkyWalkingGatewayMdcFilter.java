package com.yue.common.log.webflux;

import com.yue.common.log.CommonLogProperties;
import com.yue.common.log.skywalking.SkyWalkingTraceIds;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关下将 SkyWalking trace id 放入 Reactor Context，由 {@link TraceIdContextLifter} 同步到 MDC；不添加 trace-id 请求头，由 Agent 传 sw8。
 */
public class SkyWalkingGatewayMdcFilter implements GlobalFilter, Ordered {

    private final CommonLogProperties properties;

    public SkyWalkingGatewayMdcFilter(CommonLogProperties properties) {
        this.properties = properties;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        String traceId = SkyWalkingTraceIds.currentOrNull();
        if (properties.isResponseHeader() && traceId != null) {
            exchange.getResponse().getHeaders().add(properties.getHeaderName(), traceId);
        }
        if (traceId == null) {
            return chain.filter(exchange);
        }
        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(TraceIdContextLifter.TRACE_ID_KEY, traceId));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
