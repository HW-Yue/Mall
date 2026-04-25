package com.yue.common.log.skywalking;

import org.apache.skywalking.apm.toolkit.trace.TraceContext;

/**
 * 从 SkyWalking 取当前 trace id，过滤占位/无效值，供 MDC 与响应头使用。
 */
public final class SkyWalkingTraceIds {

    private static final String NOT_AVAILABLE = "N/A";
    private static final String IGNORED_TRACE = "Ignored_Trace";

    private SkyWalkingTraceIds() {
    }

    /**
     * @return 有效 trace id，若无上下文或非采样则返回 null
     */
    public static String currentOrNull() {
        String id = TraceContext.traceId();
        if (id == null || id.isEmpty() || NOT_AVAILABLE.equals(id) || IGNORED_TRACE.equals(id)) {
            return null;
        }
        return id;
    }
}
