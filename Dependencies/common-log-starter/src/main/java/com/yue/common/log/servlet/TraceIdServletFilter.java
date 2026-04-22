package com.yue.common.log.servlet;

import com.yue.common.log.CommonLogConstants;
import com.yue.common.log.CommonLogProperties;
import com.yue.common.log.TraceIdContext;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet 环境 TraceId 过滤器
 *
 * <p>从 HTTP Header 接收上游传递的 trace-id，放入 MDC。
 * 如果 Header 中没有，则生成新的 trace-id。
 *
 * <p>注意：在 finally 块中必须调用 MDC.clear()，防止线程复用导致 trace-id 污染。
 */
@Slf4j
public class TraceIdServletFilter implements Filter {

    private final CommonLogProperties properties;

    public TraceIdServletFilter(CommonLogProperties properties) {
        this.properties = properties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String traceId = null;

        try {
            // 1. 尝试从 HTTP Header 获取 trace-id
            traceId = httpRequest.getHeader(CommonLogConstants.HEADER_NAME);

            // 2. 如果没有，生成新的
            if (traceId == null || traceId.isEmpty()) {
                traceId = TraceIdContext.generate();
                if (log.isDebugEnabled()) {
                    log.debug("[TraceId] 未从 Header 获取到 trace-id，生成新值: {}", traceId);
                }
            }

            // 3. 放入 MDC，后续日志自动带 trace-id
            TraceIdContext.set(traceId);

            // 4. 如果需要，在响应头中返回 trace-id（方便前端排查）
            if (properties.isResponseHeader()) {
                httpResponse.setHeader(CommonLogConstants.HEADER_NAME, traceId);
            }

            // 5. 继续过滤器链
            chain.doFilter(request, response);

        } finally {
            // 6. 【重要】清理 MDC，防止线程池复用导致 trace-id 污染
            TraceIdContext.clear();
        }
    }

}
