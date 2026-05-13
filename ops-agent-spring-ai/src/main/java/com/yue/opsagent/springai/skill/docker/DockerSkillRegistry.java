package com.yue.opsagent.springai.skill.docker;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.support.SkillToolHelp;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class DockerSkillRegistry implements OpsSkillRegistry {

    public static final String SKILL_NAME = "docker_ops";

    private static final Set<String> DATA_TOOLS = Set.of("docker_logs", "docker_stats", "docker_inspect", "docker_exec");

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
        return DockerToolDocumentation.aggregatePromptFragment();
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
    public Set<String> toolNames() {
        return SkillToolHelp.toolNamesWithHelp(DATA_TOOLS, this);
    }

    @Override
    public String documentationForDataTool(String dataToolName) {
        return DockerToolDocumentation.docFor(dataToolName);
    }

    @Override
    public ToolResult execute(String toolName, Map<String, Object> args) {
        ToolResult help = SkillToolHelp.tryExecute(this, toolName, args);
        if (help != null) {
            return help;
        }
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
