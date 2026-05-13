package com.yue.opsagent.springai.skill.nacos;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.support.SkillToolHelp;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class NacosSkillRegistry implements OpsSkillRegistry {

    public static final String SKILL_NAME = "nacos_config";

    private static final Set<String> DATA_TOOLS = Set.of(
            "nacos_get_config",
            "nacos_publish_config",
            "nacos_list_instances",
            "nacos_list_services",
            "nacos_get_services");

    private final NacosToolkit toolkit;

    public NacosSkillRegistry(NacosToolkit toolkit) {
        this.toolkit = toolkit;
    }

    @Override
    public String name() {
        return SKILL_NAME;
    }

    @Override
    public String description() {
        return "Nacos：配置读写（写需审批）、服务发现查询";
    }

    @Override
    public String promptFragment() {
        return NacosToolDocumentation.aggregatePromptFragment();
    }

    @Override
    public String toolMenuBrief() {
        return """
                - nacos_get_config: 读取配置
                - nacos_publish_config: 发布配置（需审批）
                - nacos_list_instances: 健康实例列表
                - nacos_list_services / nacos_get_services: 分页服务名列表
                """;
    }

    @Override
    public Set<String> toolNames() {
        return SkillToolHelp.toolNamesWithHelp(DATA_TOOLS, this);
    }

    @Override
    public String documentationForDataTool(String dataToolName) {
        return NacosToolDocumentation.docFor(dataToolName);
    }

    @Override
    public boolean requiresApproval(String toolName) {
        return "nacos_publish_config".equals(toolName);
    }

    @Override
    public ToolResult execute(String toolName, Map<String, Object> args) {
        ToolResult help = SkillToolHelp.tryExecute(this, toolName, args);
        if (help != null) {
            return help;
        }
        Map<String, Object> a = args == null ? Map.of() : args;
        return switch (toolName) {
            case "nacos_get_config" -> toolkit.getConfig(str(a, "dataId"), str(a, "group"));
            case "nacos_publish_config" -> toolkit.publishConfig(str(a, "dataId"), str(a, "group"), str(a, "content"));
            case "nacos_list_instances" -> toolkit.listInstances(str(a, "serviceName"), str(a, "group"));
            case "nacos_list_services", "nacos_get_services" -> toolkit.listServices(
                    intArg(a, "pageNo", 1), intArg(a, "pageSize", 100));
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
