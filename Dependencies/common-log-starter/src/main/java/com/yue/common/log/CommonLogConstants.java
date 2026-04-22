package com.yue.common.log;

/**
 * 全链路日志常量
 */
public class CommonLogConstants {

    /**
     * 默认 HTTP Header 名
     */
    public static final String DEFAULT_HEADER_NAME = "trace-id";

    /**
     * 默认 MDC Key
     */
    public static final String DEFAULT_MDC_KEY = "trace-id";

    /**
     * 当前使用的 Header 名（会被 CommonLogProperties 覆盖）
     */
    public static String HEADER_NAME = DEFAULT_HEADER_NAME;

    /**
     * 当前使用的 MDC Key（会被 CommonLogProperties 覆盖）
     */
    public static String MDC_KEY = DEFAULT_MDC_KEY;

    /**
     * 初始化常量（在自动配置时调用）
     */
    public static void init(CommonLogProperties properties) {
        HEADER_NAME = properties.getHeaderName();
        MDC_KEY = properties.getMdcKey();
    }

}
