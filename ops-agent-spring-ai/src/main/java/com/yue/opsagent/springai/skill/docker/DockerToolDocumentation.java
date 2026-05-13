package com.yue.opsagent.springai.skill.docker;

import java.util.Arrays;
import java.util.stream.Collectors;

enum DockerToolDocumentation {

    docker_logs("""
            container (string 必填), tail (number 可选，默认 100)。
            拉取容器日志。"""),

    docker_stats("""
            container (string 必填)。
            容器资源统计。"""),

    docker_inspect("""
            container (string 必填)。
            容器元数据 / inspect；not found、No such container 表示服务未部署或容器名不匹配，可作为关键证据。"""),

    docker_exec("""
            container (string 必填), command (string 必填)。
            在容器内执行 sh -c 包装的单次命令（受控 shell）。""");

    private final String usage;

    DockerToolDocumentation(String usage) {
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
        return tools
                + "\n\n"
                + """
                服务存在性优先：
                - 若任务是确认服务是否部署，优先 docker_inspect container=<application 或 instance 中的容器名>。
                - inspect/logs/stats 返回 not found、No such container 或空结果时，直接报告“Docker 未发现该服务容器”，不要继续执行无关命令。
                - 只有容器存在后，才查 docker_logs / docker_stats。
                """;
    }
}
