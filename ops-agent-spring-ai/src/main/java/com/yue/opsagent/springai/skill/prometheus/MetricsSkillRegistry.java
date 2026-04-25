package com.yue.opsagent.springai.skill.prometheus;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 统一指标域：底层为 Prometheus HTTP API；工具名按场景拆分，均可传 promql 覆盖默认示例查询。
 */
@Component
public class MetricsSkillRegistry implements OpsSkillRegistry {

    public static final String SKILL_NAME = "metrics_ops";

    private final PrometheusToolkit toolkit;

    public MetricsSkillRegistry(PrometheusToolkit toolkit) {
        this.toolkit = toolkit;
    }

    @Override
    public String name() {
        return SKILL_NAME;
    }

    @Override
    public String description() {
        return "统一指标（Prometheus）：Sentinel / DynamicTP / JVM / 业务 PromQL";
    }

    @Override
    public String promptFragment() {
        return """
                工具（args 均可选 promql:string 覆盖默认值）：
                - sentinel_metrics: Sentinel 相关示例查询
                - dynamictp_metrics: 线程池 / DynamicTP 相关示例查询
                - jvm_metrics: JVM 进程指标示例查询
                - business_metrics: 业务自定义指标示例查询

                服务存在性优先：
                - 确认服务是否被 Prometheus 采集时，优先查 up{application="<app>"}、up{app="<app>"}、up{job="<app>"}。
                - 如果这些查询 result 为空，直接报告“Prometheus 未发现该服务时序/标签可能不一致”，不要继续猜业务指标名。
                - 若要查 5xx，再基于已有标签查 http_server_requests_seconds_count / *_requests_total 等；没有基础 up 时先返回不存在证据。
                """;
    }

    @Override
    public String toolMenuBrief() {
        return """
                - sentinel_metrics: Sentinel / 限流熔断相关 PromQL
                - dynamictp_metrics: 线程池指标 PromQL
                - jvm_metrics: JVM PromQL
                - business_metrics: 业务指标 PromQL
                """;
    }

    @Override
    public String toolSpecification(String toolName) {
        return switch (toolName) {
            case "sentinel_metrics" -> "sentinel_metrics：args 可选 promql，默认 up";
            case "dynamictp_metrics" -> "dynamictp_metrics：args 可选 promql，默认 process_threads";
            case "jvm_metrics" -> "jvm_metrics：args 可选 promql，默认 jvm_memory_used_bytes";
            case "business_metrics" -> """
                    business_metrics：args 可选 promql，默认 up。
                    服务存在性示例：
                    up{application="<app>"} or up{app="<app>"} or up{job="<app>"}
                    """;
            default -> OpsSkillRegistry.super.toolSpecification(toolName);
        };
    }

    @Override
    public Set<String> toolNames() {
        return Set.of("sentinel_metrics", "dynamictp_metrics", "jvm_metrics", "business_metrics");
    }

    @Override
    public ToolResult execute(String toolName, Map<String, Object> args) {
        String override = promql(args);
        return switch (toolName) {
            case "sentinel_metrics" -> toolkit.queryInstant(!override.isBlank() ? override : "up");
            case "dynamictp_metrics" -> toolkit.queryInstant(!override.isBlank() ? override : "process_threads");
            case "jvm_metrics" -> toolkit.queryInstant(!override.isBlank() ? override : "jvm_memory_used_bytes");
            case "business_metrics" -> toolkit.queryInstant(!override.isBlank() ? override : "up");
            default -> ToolResult.error("unknown tool: " + toolName);
        };
    }

    private static String promql(Map<String, Object> args) {
        if (args == null || args.get("promql") == null) {
            return "";
        }
        return String.valueOf(args.get("promql")).trim();
    }
}
