package com.yue.opsagent.springai.skill.elasticsearch;

import java.util.Arrays;
import java.util.stream.Collectors;

enum ElasticsearchToolDocumentation {

    es_indices("""
            cluster (string 可选)：logs（默认，应用日志 ELK）或 skywalking / sw（SkyWalking 存储，索引如 sw_segment-*、sw_metrics-*）。
            列出索引。"""),

    es_search("""
            index (string 必填), query (string 可选), cluster (string 可选，含义同 es_indices)。
            DSL 搜索。"""),

    es_count("""
            index (string 必填), query (string 可选), cluster (string 可选)。
            文档计数。"""),

    es_aggregation("""
            index (string 必填), body (string 必填), cluster (string 可选)。
            terms / date_histogram 等聚合；body 为 JSON 字符串。""");

    private final String usage;

    ElasticsearchToolDocumentation(String usage) {
        this.usage = usage.trim();
    }

    static String docFor(String toolId) {
        try {
            return valueOf(toolId).usage;
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    static String aggregatePromptFragment() {
        String header = """
                参数均为 JSON / Map。可选 cluster：logs（默认，应用日志 ELK）或 skywalking / sw（SkyWalking 存储，索引如 sw_segment-*、sw_metrics-*）。

                """;
        String tools = Arrays.stream(values())
                .map(e -> "- " + e.name() + ":\n" + e.usage)
                .collect(Collectors.joining("\n\n"));
        return header + tools + "\n";
    }
}
