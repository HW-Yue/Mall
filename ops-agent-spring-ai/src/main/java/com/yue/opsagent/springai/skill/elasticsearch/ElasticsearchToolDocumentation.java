package com.yue.opsagent.springai.skill.elasticsearch;

import java.util.Arrays;
import java.util.stream.Collectors;

enum ElasticsearchToolDocumentation {

    es_indices("""
            cluster (string 可选)：logs（默认，应用日志 ELK）或 skywalking / sw（SkyWalking 存储，索引如 sw_segment-*、sw_metrics-*）。
            列出索引。"""),

    es_search_service_errors("""
            service (string 可选), application (string 可选), lookback (string 可选，默认 1h), size (number 可选，默认 10，最大 20),
            keywords (array/string 可选), index (string 可选，默认 nexus-*), cluster (string 可选，含义同 es_indices)。
            按服务检索错误日志摘要。默认优先使用本工具，不要手写 DSL。service / application 至少提供一个。"""),

    es_search("""
            index (string 必填), query (string 可选), cluster (string 可选，含义同 es_indices)。
            DSL 搜索。query 可以是完整 search body，也可以是 bare query clause（如 {"bool":{...}}），工具会自动补成 {"query":...}。"""),

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
        return header + tools + "\n\n"
                + """
                推荐顺序：
                - 文本排查默认先用 es_search_service_errors，按服务名和时间窗口看错误样本。
                - 只有高层错误日志入口不足以回答问题时，才改用 es_search / es_count / es_aggregation。
                - 不要先手写复杂 DSL；如果只是写 bool/term/range/query_string，直接交给 es_search_service_errors 或让 es_search 自动包 query。
                """;
    }
}
