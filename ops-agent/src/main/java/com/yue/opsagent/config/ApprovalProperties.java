package com.yue.opsagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批流 / 告警联动配置，绑定 {@code ops-agent.approval.*}
 * <p>
 * 示例：
 * <pre>
 * ops-agent:
 *   approval:
 *     pending-ttl: PT30M
 *     throttle-window: PT5M
 *     sensitive-tools: [ publishConfig ]
 *     strategies:
 *       sentinel:
 *         group: SENTINEL_GROUP
 *         data-id-template: "${app}-flow-rules.json"
 *         alerts: [ SentinelBlockRateHigh, ... ]
 *       dtp:
 *         group: DEFAULT_GROUP
 *         data-id-template: "${app}-dtp-${profile}.yml"
 *         extra: { profile: dev }
 *         alerts: [ ThreadPoolAtMaxCapacity, ... ]
 *       notify:
 *         alerts: [ JvmHeapUsageHigh, ServiceDown, ... ]
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "ops-agent.approval")
public class ApprovalProperties {

    /** PENDING 任务超过该时长未处理则自动置 EXPIRED。 */
    private Duration pendingTtl = Duration.ofMinutes(30);

    /** 同一个 alertKey 在该窗口内只触发一次 Agent（防风暴）。 */
    private Duration throttleWindow = Duration.ofMinutes(5);

    /**
     * 旧版扁平白名单，保留作兼容兜底；当 {@link #strategies} 里所有策略都未命中时，
     * 如果 alertname 在这里出现，会按 {@code sentinel} 域兜底处理（与历史行为一致）。
     *
     * @deprecated 请改用 {@link #strategies} 下按域配置的 alerts 列表
     */
    @Deprecated
    private List<String> alertWhitelist = new ArrayList<>();

    /** 哪些 tool 名进入审批流（由 publishConfig 这类写操作触发）。 */
    private List<String> sensitiveTools = List.of("publishConfig");

    /** 按域组织的策略配置。key 必须与 {@code AlertStrategy#domain()} 返回值一致。 */
    private Map<String, StrategyProps> strategies = new LinkedHashMap<>();

    public Duration getPendingTtl() {
        return pendingTtl;
    }

    public void setPendingTtl(Duration pendingTtl) {
        this.pendingTtl = pendingTtl;
    }

    public Duration getThrottleWindow() {
        return throttleWindow;
    }

    public void setThrottleWindow(Duration throttleWindow) {
        this.throttleWindow = throttleWindow;
    }

    @Deprecated
    public List<String> getAlertWhitelist() {
        return alertWhitelist;
    }

    @Deprecated
    public void setAlertWhitelist(List<String> alertWhitelist) {
        this.alertWhitelist = alertWhitelist;
    }

    public List<String> getSensitiveTools() {
        return sensitiveTools;
    }

    public void setSensitiveTools(List<String> sensitiveTools) {
        this.sensitiveTools = sensitiveTools;
    }

    public Map<String, StrategyProps> getStrategies() {
        return strategies;
    }

    public void setStrategies(Map<String, StrategyProps> strategies) {
        this.strategies = strategies;
    }

    /**
     * 单个域的策略配置。公共字段覆盖 Sentinel / DTP / MySQL / Redis / RocketMQ 等常见需求；
     * 仅某一两个域需要的字段可放进 {@link #extra}，无须为了新字段改 schema。
     */
    public static class StrategyProps {

        /** 告警白名单；不在列表里的 alertname 即便命中 category，也不交给该策略。 */
        private List<String> alerts = new ArrayList<>();

        /** Nacos group；sentinel 通常 SENTINEL_GROUP，dtp 通常 DEFAULT_GROUP。 */
        private String group;

        /**
         * Nacos dataId 模板，占位符形如 {@code ${app}} / {@code ${profile}}；
         * 由策略自行解析替换。如果策略不需要写 Nacos（如 notify），可以为 null。
         */
        private String dataIdTemplate;

        /**
         * 该域专用的附加字段，比如 dtp 的 {@code profile=dev}、redis 的 {@code namespace}。
         * 通过 {@code extra.getOrDefault("profile", "dev")} 读取，新字段无须改 Java 类。
         */
        private Map<String, String> extra = new LinkedHashMap<>();

        public List<String> getAlerts() {
            return alerts;
        }

        public void setAlerts(List<String> alerts) {
            this.alerts = alerts;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getDataIdTemplate() {
            return dataIdTemplate;
        }

        public void setDataIdTemplate(String dataIdTemplate) {
            this.dataIdTemplate = dataIdTemplate;
        }

        public Map<String, String> getExtra() {
            return extra;
        }

        public void setExtra(Map<String, String> extra) {
            this.extra = extra;
        }
    }
}
