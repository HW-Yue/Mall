package com.yue.opsagent.springai.skill.catalog;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.support.SkillToolHelp;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class CatalogSkillRegistry implements OpsSkillRegistry {

    public static final String SKILL_NAME = "catalog_ops";

    private static final Set<String> DATA_TOOLS =
            Set.of(
                    "catalog_resolve_service",
                    "catalog_list_services",
                    "catalog_list_topics",
                    "catalog_describe_service",
                    "catalog_lookup_resource_owner");

    private final CatalogToolkit toolkit;

    public CatalogSkillRegistry(CatalogToolkit toolkit) {
        this.toolkit = toolkit;
    }

    @Override
    public String name() {
        return SKILL_NAME;
    }

    @Override
    public String description() {
        return "静态服务知识库：服务/application/compose/container 以及 resource/topic/table/pool 归属";
    }

    @Override
    public String promptFragment() {
        return CatalogToolDocumentation.aggregatePromptFragment();
    }

    @Override
    public String toolMenuBrief() {
        return """
                - catalog_resolve_service: 文本或线索归因到标准服务
                - catalog_list_services: 列出当前静态知识库中的服务名
                - catalog_list_topics: 列出当前静态知识库中的 Topic 名
                - catalog_describe_service: 查看服务静态拓扑
                - catalog_lookup_resource_owner: 按 resource/topic/table/pool 反查归属
                """;
    }

    @Override
    public Set<String> toolNames() {
        return SkillToolHelp.toolNamesWithHelp(DATA_TOOLS, this);
    }

    @Override
    public String documentationForDataTool(String dataToolName) {
        return CatalogToolDocumentation.docFor(dataToolName);
    }

    @Override
    public ToolResult execute(String toolName, Map<String, Object> args) {
        ToolResult help = SkillToolHelp.tryExecute(this, toolName, args);
        if (help != null) {
            return help;
        }
        Map<String, Object> a = args == null ? Map.of() : args;
        return switch (toolName) {
            case "catalog_resolve_service" -> toolkit.resolveService(
                    str(a, "query"),
                    str(a, "service"),
                    str(a, "application"),
                    str(a, "resource"),
                    str(a, "topic"),
                    str(a, "consumerGroup"),
                    str(a, "table"),
                    str(a, "database"),
                    str(a, "pool"));
            case "catalog_list_services" -> toolkit.listServices();
            case "catalog_list_topics" -> toolkit.listTopics();
            case "catalog_describe_service" -> toolkit.describeService(str(a, "service"));
            case "catalog_lookup_resource_owner" -> toolkit.lookupOwner(str(a, "kind"), str(a, "value"));
            default -> ToolResult.error("unknown tool: " + toolName);
        };
    }

    private static String str(Map<String, Object> args, String key) {
        Object value = args.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
