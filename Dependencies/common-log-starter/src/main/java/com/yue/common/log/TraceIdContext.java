package com.yue.common.log;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * TraceId 上下文工具类
 * 提供生成、获取、设置、清理 TraceId 的统一入口
 */
public class TraceIdContext {

    /**
     * 生成新的 TraceId
     */
    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 从 MDC 获取当前 TraceId
     */
    public static String get() {
        return MDC.get(CommonLogConstants.MDC_KEY);
    }

    /**
     * 设置 TraceId 到 MDC
     */
    public static void set(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put(CommonLogConstants.MDC_KEY, traceId);
        }
    }

    /**
     * 获取或生成 TraceId
     * 如果 MDC 中已存在则返回，否则生成新的
     */
    public static String getOrGenerate() {
        String traceId = get();
        if (traceId == null || traceId.isEmpty()) {
            traceId = generate();
            set(traceId);
        }
        return traceId;
    }

    /**
     * 清理 MDC 中的 TraceId
     * 必须在 Filter 的 finally 块中调用，防止内存泄漏
     */
    public static void clear() {
        MDC.remove(CommonLogConstants.MDC_KEY);
    }

    /**
     * 获取 HTTP Header 名
     */
    public static String getHeaderName() {
        return CommonLogConstants.HEADER_NAME;
    }

    /**
     * 获取 MDC Key
     */
    public static String getMdcKey() {
        return CommonLogConstants.MDC_KEY;
    }

}
