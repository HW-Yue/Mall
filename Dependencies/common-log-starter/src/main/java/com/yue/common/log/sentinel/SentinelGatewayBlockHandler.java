package com.yue.common.log.sentinel;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Cloud Gateway（WebFlux）下的 Sentinel 限流异常处理器。
 * <p>通过 {@link GatewayCallbackManager#setBlockHandler} 注册。
 * <p>规则触发后返回 JSON，不让前端看到 500。
 */
@Slf4j
@RequiredArgsConstructor
public class SentinelGatewayBlockHandler implements BlockRequestHandler {

    private final SentinelCommonProperties properties;

    @PostConstruct
    public void register() {
        GatewayCallbackManager.setBlockHandler(this);
        log.info("[Sentinel] Gateway BlockRequestHandler registered, httpStatus={}, code={}",
                properties.getBlockHttpStatus(), properties.getBlockCode());
    }

    @Override
    public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable t) {
        String blockType = resolveBlockType(t);
        String resource = exchange.getRequest().getPath().value();

        Map<String, Object> body = new LinkedHashMap<>(4);
        body.put("code", properties.getBlockCode());
        body.put("info", properties.getBlockMessage());
        body.put("blockType", blockType);
        body.put("resource", resource);

        return ServerResponse.status(HttpStatusCode.valueOf(properties.getBlockHttpStatus()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body));
    }

    private String resolveBlockType(Throwable t) {
        if (t instanceof FlowException) {
            return "flow";
        } else if (t instanceof DegradeException) {
            return "degrade";
        } else if (t instanceof ParamFlowException) {
            return "param-flow";
        } else if (t instanceof SystemBlockException) {
            return "system";
        } else if (t instanceof BlockException) {
            return "block";
        }
        return "unknown";
    }
}
