package com.yue.opsagent.springai.skill.prometheus;

import java.util.Arrays;
import java.util.stream.Collectors;

enum MetricsToolDocumentation {

    sentinel_metrics("""
            promql (string 可选)：覆盖默认示例查询。默认示例面向 Sentinel / 限流熔断相关 up 类查询。
            """),

    dynamictp_metrics("""
            promql (string 可选)：覆盖默认示例。默认可用 process_threads 等线程池相关示例。"""),

    jvm_metrics("""
            promql (string 可选)：覆盖默认示例。默认可用 jvm_memory_used_bytes 等 JVM 指标。"""),

    business_metrics("""
            promql (string 可选)：覆盖默认示例；默认可用 up。
            服务存在性示例：up{application="<app>"} or up{app="<app>"} or up{job="<app>"}
            """);

    private final String usage;

    MetricsToolDocumentation(String usage) {
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
        String tools = Arrays.stream(values())
                .map(e -> "- " + e.name() + ":\n" + e.usage)
                .collect(Collectors.joining("\n\n"));
        return """
                工具（args 均可选 promql:string 覆盖默认值）：

                """
                + tools
                + "\n\n"
                + """
                服务存在性优先：
                - 确认服务是否被 Prometheus 采集时，优先查 up{application="<app>"}、up{app="<app>"}、up{job="<app>"}。
                - 如果这些查询 result 为空，直接报告“Prometheus 未发现该服务时序/标签可能不一致”，不要继续猜业务指标名。
                - 若要查 5xx，再基于已有标签查 http_server_requests_seconds_count / *_requests_total 等；没有基础 up 时先返回不存在证据。
                """;
    }
}
