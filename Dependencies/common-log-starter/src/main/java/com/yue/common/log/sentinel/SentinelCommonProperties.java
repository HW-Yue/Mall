package com.yue.common.log.sentinel;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * common-log-starter 提供的 Sentinel 增强配置。
 * <p>真正的 Sentinel 核心配置仍由 {@code spring.cloud.sentinel.*}（SCA 原生）管理，
 * 这里只控制统一 BlockHandler 和 Micrometer 指标导出。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "yue.sentinel")
public class SentinelCommonProperties {

    /** 是否启用 common-log-starter 提供的 Sentinel 增强（默认开启） */
    private boolean enabled = true;

    /** 限流/熔断触发时返回给前端的 HTTP 状态码 */
    private int blockHttpStatus = 429;

    /** 限流/熔断触发时返回给前端的业务码 */
    private String blockCode = "429";

    /** 限流/熔断触发时返回给前端的提示语 */
    private String blockMessage = "系统繁忙，请稍后再试";

    /** 是否把 Sentinel 的 ClusterNode 统计作为 Micrometer Gauge 暴露到 /actuator/prometheus */
    private boolean metricsEnabled = true;

    /** Micrometer 指标采集周期（毫秒），默认 1s */
    private long metricsIntervalMs = 1000L;
}
