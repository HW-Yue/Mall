package com.yue.opsagent.springai.skill.nacos;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class NacosSkillRegistry implements OpsSkillRegistry {

    public static final String SKILL_NAME = "nacos_config";

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
        return """
                工具：
                - nacos_get_config: dataId(string), group(string 可选，默认 DEFAULT_GROUP)
                - nacos_publish_config: dataId(string), group(string 可选), content(string) — 高危，需人工审批
                - nacos_list_instances: serviceName(string), group(string 可选)
                - nacos_list_services / nacos_get_services: pageNo(number 可选), pageSize(number 可选)（别名）

                服务存在性优先：
                - 排查服务错误率/服务不可用时，先用 nacos_list_instances 查询告警 application/serviceName。
                - 若实例列表为空或无健康实例，直接报告“服务未注册或无健康实例”，不要继续读无关配置。
                - 只有确认服务存在且问题指向配置时，才使用 nacos_get_config；nacos_publish_config 必须等待人工审批。
                """;
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
    public String toolSpecification(String toolName) {
        return switch (toolName) {
            case "nacos_get_config" -> """
                    nacos_get_config
                    参数 JSON 键：
                    - dataId (string, 必填)
                    - group (string, 可选，默认 DEFAULT_GROUP)
                    """;
            case "nacos_publish_config" -> """
                    nacos_publish_config（高危，可能触发审批挂起）
                    参数 JSON 键：
                    - dataId (string, 必填)
                    - group (string, 可选)
                    - content (string, 配置正文)
                    """;
            case "nacos_list_instances" -> """
                    nacos_list_instances
                    参数：serviceName (string 必填), group (string 可选)。
                    空实例列表是关键证据：表示服务未注册、名称不一致或当前无健康实例。
                    """;
            case "nacos_list_services", "nacos_get_services" -> """
                    nacos_list_services / nacos_get_services
                    参数：pageNo (number 可选), pageSize (number 可选)
                    """;
            default -> OpsSkillRegistry.super.toolSpecification(toolName);
        };
    }

    @Override
    public Set<String> toolNames() {
        return Set.of(
                "nacos_get_config",
                "nacos_publish_config",
                "nacos_list_instances",
                "nacos_list_services",
                "nacos_get_services");
    }

    @Override
    public boolean requiresApproval(String toolName) {
        return "nacos_publish_config".equals(toolName);
    }

    @Override
    public ToolResult execute(String toolName, Map<String, Object> args) {
        Map<String, Object> a = args == null ? Map.of() : args;
        return switch (toolName) {
            case "nacos_get_config" -> toolkit.getConfig(
                    str(a, "dataId"),
                    str(a, "group"));
            case "nacos_publish_config" -> toolkit.publishConfig(
                    str(a, "dataId"),
                    str(a, "group"),
                    str(a, "content"));
            case "nacos_list_instances" -> toolkit.listInstances(
                    str(a, "serviceName"),
                    str(a, "group"));
            case "nacos_list_services", "nacos_get_services" -> toolkit.listServices(
                    intArg(a, "pageNo", 1),
                    intArg(a, "pageSize", 100));
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
