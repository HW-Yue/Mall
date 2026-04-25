package com.yue.common.log.skywalking;

import com.yue.common.log.CommonLogProperties;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;

/**
 * 将 {@link org.apache.skywalking.apm.toolkit.trace.TraceContext} 的 trace id 同步到 MDC，便于 Logback/Logstash 字段名不变。
 * <p>执行顺序靠后，以便 SkyWalking Agent 已创建 entry span（具体顺序可按环境再调）。
 */
public class SkyWalkingTraceMdcFilter extends OncePerRequestFilter {

    private final CommonLogProperties properties;

    public SkyWalkingTraceMdcFilter(CommonLogProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String traceId = SkyWalkingTraceIds.currentOrNull();
        String mdcKey = properties.getMdcKey();
        try {
            if (traceId != null) {
                MDC.put(mdcKey, traceId);
            }
            if (properties.isResponseHeader() && traceId != null) {
                response.setHeader(properties.getHeaderName(), traceId);
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(mdcKey);
        }
    }
}
