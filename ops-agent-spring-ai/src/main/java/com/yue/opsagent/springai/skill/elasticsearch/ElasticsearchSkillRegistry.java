package com.yue.opsagent.springai.skill.elasticsearch;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.support.SkillToolHelp;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class ElasticsearchSkillRegistry implements OpsSkillRegistry {

    public static final String SKILL_NAME = "elasticsearch_ops";

    private static final Set<String> DATA_TOOLS = Set.of(
            "es_indices",
            "es_search_service_errors",
            "es_search",
            "es_count",
            "es_aggregation");

    private final ElasticsearchToolkit toolkit;

    public ElasticsearchSkillRegistry(ElasticsearchToolkit toolkit) {
        this.toolkit = toolkit;
    }

    @Override
    public String name() {
        return SKILL_NAME;
    }

    @Override
    public String description() {
        return "Elasticsearch 只读：按服务检索错误日志、索引、搜索、计数、聚合（应用日志 nexus-* 与 SkyWalking OAP 的 sw_*，可选独立 ES 地址）";
    }

    @Override
    public String promptFragment() {
        return ElasticsearchToolDocumentation.aggregatePromptFragment();
    }

    @Override
    public String toolMenuBrief() {
        return """
                - es_indices: 列出索引（可选 cluster=skywalking）
                - es_search_service_errors: 按服务检索错误日志（默认入口，优先使用）
                - es_search: DSL 搜索
                - es_count: 文档计数
                - es_aggregation: terms/date_histogram 等聚合
                """;
    }

    @Override
    public Set<String> toolNames() {
        return SkillToolHelp.toolNamesWithHelp(DATA_TOOLS, this);
    }

    @Override
    public String documentationForDataTool(String dataToolName) {
        return ElasticsearchToolDocumentation.docFor(dataToolName);
    }

    @Override
    public ToolResult execute(String toolName, Map<String, Object> args) {
        ToolResult help = SkillToolHelp.tryExecute(this, toolName, args);
        if (help != null) {
            return help;
        }
        String cluster = clusterArg(args);
        return switch (toolName) {
            case "es_indices" -> toolkit.listIndices(cluster);
            case "es_search_service_errors" -> toolkit.searchServiceErrors(
                    cluster,
                    stringArg(args, "index"),
                    stringArg(args, "service"),
                    stringArg(args, "application"),
                    stringArg(args, "lookback"),
                    intArg(args, "size", 10),
                    listArg(args, "keywords"));
            case "es_search" -> toolkit.search(cluster, stringArg(args, "index"), stringArg(args, "query"));
            case "es_count" -> toolkit.count(cluster, stringArg(args, "index"), stringArg(args, "query"));
            case "es_aggregation" -> toolkit.aggregate(cluster, stringArg(args, "index"), stringArg(args, "body"));
            default -> ToolResult.error("unknown tool: " + toolName);
        };
    }

    private static String clusterArg(Map<String, Object> args) {
        String c = stringArg(args, "cluster");
        return c.isBlank() ? "logs" : c;
    }

    private static String stringArg(Map<String, Object> args, String key) {
        if (args == null || !args.containsKey(key) || args.get(key) == null) {
            return "";
        }
        return String.valueOf(args.get(key));
    }

    private static int intArg(Map<String, Object> args, String key, int def) {
        if (args == null) {
            return def;
        }
        Object value = args.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return def;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static java.util.List<String> listArg(Map<String, Object> args, String key) {
        if (args == null) {
            return java.util.List.of();
        }
        Object value = args.get(key);
        if (value instanceof java.util.List<?> list) {
            return list.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(String::valueOf)
                    .toList();
        }
        if (value == null) {
            return java.util.List.of();
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(text.split("[,，;；|]+"))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
    }
}
