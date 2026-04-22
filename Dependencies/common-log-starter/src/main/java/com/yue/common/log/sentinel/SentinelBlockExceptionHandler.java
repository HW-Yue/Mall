package com.yue.common.log.sentinel;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.alibaba.fastjson.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring MVC（Servlet）下的统一 Sentinel 限流异常处理器。
 * <p>当 Sentinel 抛出 {@link BlockException} 时，返回统一 JSON 结构给前端，避免直接暴露 500。
 * <p>通过 {@code spring-cloud-starter-alibaba-sentinel} 注册的 {@code SentinelWebInterceptor}
 * 会自动拾取容器内的 {@link BlockExceptionHandler} Bean。
 */
@Slf4j
@RequiredArgsConstructor
public class SentinelBlockExceptionHandler implements BlockExceptionHandler {

    private final SentinelCommonProperties properties;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       String resourceName, BlockException e) throws Exception {
        String blockType = resolveBlockType(e);

        if (log.isDebugEnabled()) {
            log.debug("[Sentinel] block triggered, resource={}, type={}, rule={}",
                    resourceName, blockType, e.getRule());
        }

        Map<String, Object> body = new LinkedHashMap<>(4);
        body.put("code", properties.getBlockCode());
        body.put("info", properties.getBlockMessage());
        body.put("blockType", blockType);
        body.put("resource", resourceName);

        response.setStatus(properties.getBlockHttpStatus());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.print(JSON.toJSONString(body));
            writer.flush();
        }
    }

    private String resolveBlockType(BlockException e) {
        if (e instanceof FlowException) {
            return "flow";
        } else if (e instanceof DegradeException) {
            return "degrade";
        } else if (e instanceof ParamFlowException) {
            return "param-flow";
        } else if (e instanceof SystemBlockException) {
            return "system";
        } else if (e instanceof AuthorityException) {
            return "authority";
        }
        return "unknown";
    }
}
