package com.yue.common.log;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 全链路日志配置属性
 */
@Data
@ConfigurationProperties(prefix = "yue.log.trace")
public class CommonLogProperties {

    /**
     * 是否启用全链路 TraceId
     */
    private boolean enabled = true;

    /**
     * HTTP Header 中传递 TraceId 的键名
     */
    private String headerName = "trace-id";

    /**
     * MDC 中存储 TraceId 的键名
     */
    private String mdcKey = "trace-id";

    /**
     * 是否在 HTTP 响应头中返回 TraceId（方便前端排查）
     */
    private boolean responseHeader = false;

    /**
     * 是否自动注册线程池装饰器（保证异步线程也能传递 TraceId）
     */
    private boolean taskDecorator = true;

}
