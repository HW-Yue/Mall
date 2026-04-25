package com.yue.opsagent.springai.skill.docker;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class DockerSkillRegistry implements OpsSkillRegistry {

    public static final String SKILL_NAME = "docker_ops";

    private final DockerToolkit toolkit;

    public DockerSkillRegistry(DockerToolkit toolkit) {
        this.toolkit = toolkit;
    }

    @Override
    public String name() {
        return SKILL_NAME;
    }

    @Override
    public String description() {
        return "Docker 只读诊断：日志、stats、inspect、exec（受控 shell）";
    }

    @Override
    public String promptFragment() {
        return """
                - docker_logs: container(string), tail(number 可选)
                - docker_stats: container(string)
                - docker_inspect: container(string)
                - docker_exec: container(string), command(string)，在容器内执行 sh -c

                服务存在性优先：
                - 若任务是确认服务是否部署，优先 docker_inspect container=<application 或 instance 中的容器名>。
                - inspect/logs/stats 返回 not found、No such container 或空结果时，直接报告“Docker 未发现该服务容器”，不要继续执行无关命令。
                - 只有容器存在后，才查 docker_logs / docker_stats。
                """;
    }

    @Override
    public String toolMenuBrief() {
        return """
                - docker_logs: 容器日志
                - docker_stats: 资源统计
                - docker_inspect: 容器元数据
                - docker_exec: 单次命令
                """;
    }

    @Override
    public String toolSpecification(String toolName) {
        return switch (toolName) {
            case "docker_logs" -> "docker_logs: container, tail(可选)";
            case "docker_stats" -> "docker_stats: container";
            case "docker_inspect" -> "docker_inspect: container。not found 是服务未部署/容器名不匹配的关键证据。";
            case "docker_exec" -> "docker_exec: container, command";
            default -> OpsSkillRegistry.super.toolSpecification(toolName);
        };
    }

    @Override
    public Set<String> toolNames() {
        return Set.of("docker_logs", "docker_stats", "docker_inspect", "docker_exec");
    }

    @Override
    public ToolResult execute(String toolName, Map<String, Object> args) {
        Map<String, Object> a = args == null ? Map.of() : args;
        return switch (toolName) {
            case "docker_logs" -> toolkit.dockerLogs(str(a, "container"), intArg(a, "tail", 100));
            case "docker_stats" -> toolkit.dockerStats(str(a, "container"));
            case "docker_inspect" -> toolkit.dockerInspect(str(a, "container"));
            case "docker_exec" -> toolkit.dockerExec(str(a, "container"), str(a, "command"));
            default -> ToolResult.error("unknown tool: " + toolName);
        };
    }

    private static String str(Map<String, Object> a, String key) {
        Object v = a.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private static int intArg(Map<String, Object> a, String key, int def) {
        Object v = a.get(key);
        if (v == null) {
            return def;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
