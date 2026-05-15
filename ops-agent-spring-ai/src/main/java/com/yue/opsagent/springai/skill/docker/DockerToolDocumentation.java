package com.yue.opsagent.springai.skill.docker;

import java.util.Arrays;
import java.util.stream.Collectors;

enum DockerToolDocumentation {

    docker_logs("""
            container (string 必填), tail (number 可选，默认 100)。
            拉取容器日志。container 可传真实容器名，也可传 application / Docker Compose service 名；工具会尝试解析 nexus- 前缀容器。"""),

    docker_stats("""
            container (string 必填)。
            容器资源统计。container 可传真实容器名，也可传 application / Docker Compose service 名；工具会尝试解析 nexus- 前缀容器。"""),

    docker_inspect("""
            container (string 必填)。
            容器元数据 / inspect。container 可传真实容器名，也可传 application / Docker Compose service 名；工具会尝试解析 nexus- 前缀容器。解析后仍 not found / No such container，才表示服务未部署或容器名不匹配。"""),

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
                - 进入 Docker Skill 前，必须先从 Catalog 的 catalog_describe_service 结果里拿到非空 profile.containerName；没有 containerName 就停止，不要猜容器名。
                - 若任务是确认服务是否部署，优先 docker_inspect container=<application 或 instance 中的容器名>；工具会兼容 nexus-<application> 和 Compose service label。
                - inspect/logs/stats 返回 not found、No such container 或空结果时，才报告“Docker 未发现该服务容器”，不要继续执行无关命令。
                - 只有容器存在后，才查 docker_logs / docker_stats。
                """;
    }
}
