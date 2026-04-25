package com.yue.opsagent.springai.skill.elasticsearch;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class ElasticsearchSkillRegistry implements OpsSkillRegistry {

    public static final String SKILL_NAME = "elasticsearch_ops";

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
        return "Elasticsearch 只读：索引、搜索、计数、聚合（应用日志 nexus-* 与 SkyWalking OAP 的 sw_*，可选独立 ES 地址）";
    }

    @Override
    public String promptFragment() {
        return """
                工具（参数均为 JSON / Map）。可选 cluster：logs（默认，应用日志 ELK）或 skywalking / sw（SkyWalking 存储，索引如 sw_segment-*、sw_metrics-*）。
                - es_indices: cluster(string 可选)
                - es_search: index(string 必填), query(string 可选), cluster(string 可选)
                - es_count: index(string 必填), query(string 可选), cluster(string 可选)
                - es_aggregation: index(string 必填), body(string 必填), cluster(string 可选)
                """;
    }

    @Override
    public String toolMenuBrief() {
        return """
                - es_indices: 列出索引（可选 cluster=skywalking）
                - es_search: DSL 搜索
                - es_count: 文档计数
                - es_aggregation: terms/date_histogram 等聚合
                """;
    }

    @Override
    public String toolSpecification(String toolName) {
        return switch (toolName) {
            case "es_indices" -> """
                    es_indices
                    args: cluster (string 可选，logs | skywalking | sw，默认 logs)
                    """;
            case "es_search" -> """
                    es_search
                    args: index (string 必填), query (string 可选), cluster (string 可选)
                    """;
            case "es_count" -> """
                    es_count
                    args: index (string 必填), query (string 可选), cluster (string 可选)
                    """;
            case "es_aggregation" -> """
                    es_aggregation
                    args: index (string 必填), body (string 必填), cluster (string 可选)
                    """;
            default -> OpsSkillRegistry.super.toolSpecification(toolName);
        };
    }

    @Override
    public Set<String> toolNames() {
        return Set.of("es_indices", "es_search", "es_count", "es_aggregation");
    }

    @Override
    public ToolResult execute(String toolName, Map<String, Object> args) {
        String cluster = clusterArg(args);
        return switch (toolName) {
            case "es_indices" -> toolkit.listIndices(cluster);
            case "es_search" -> toolkit.search(
                    cluster,
                    stringArg(args, "index"),
                    stringArg(args, "query"));
            case "es_count" -> toolkit.count(
                    cluster,
                    stringArg(args, "index"),
                    stringArg(args, "query"));
            case "es_aggregation" -> toolkit.aggregate(
                    cluster,
                    stringArg(args, "index"),
                    stringArg(args, "body"));
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
}
