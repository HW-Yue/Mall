package com.yue.opsagent.springai.skill.prometheus;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.support.SkillToolHelp;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 统一指标域：底层为 Prometheus HTTP API；工具名按场景拆分，均可传 promql 覆盖默认示例查询。
 */
@Component
public class MetricsSkillRegistry implements OpsSkillRegistry {

    public static final String SKILL_NAME = "metrics_ops";

    private static final Set<String> DATA_TOOLS =
            Set.of("sentinel_metrics", "dynamictp_metrics", "jvm_metrics", "business_metrics");

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
        return MetricsToolDocumentation.aggregatePromptFragment();
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
    public Set<String> toolNames() {
        return SkillToolHelp.toolNamesWithHelp(DATA_TOOLS, this);
    }

    @Override
    public String documentationForDataTool(String dataToolName) {
        return MetricsToolDocumentation.docFor(dataToolName);
    }

    @Override
    public ToolResult execute(String toolName, Map<String, Object> args) {
        ToolResult help = SkillToolHelp.tryExecute(this, toolName, args);
        if (help != null) {
            return help;
        }
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
