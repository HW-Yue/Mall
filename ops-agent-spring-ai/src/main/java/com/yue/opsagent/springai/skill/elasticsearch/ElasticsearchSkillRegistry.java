package com.yue.opsagent.springai.skill.elasticsearch;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.support.SkillToolHelp;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class ElasticsearchSkillRegistry implements OpsSkillRegistry {

    public static final String SKILL_NAME = "elasticsearch_ops";

    private static final Set<String> DATA_TOOLS = Set.of("es_indices", "es_search", "es_count", "es_aggregation");

    private final ElasticsearchToolkit toolkit;

    public ElasticsearchSkillRegistry(ElasticsearchToolkit toolkit) {
        this.toolkit = toolkit;
    }

    @Override
    public String name() {
        return SKILL_NAME;
    }

    @Override
    public String description() {
        return "Elasticsearch 只读：索引、搜索、计数、聚合（应用日志 nexus-* 与 SkyWalking OAP 的 sw_*，可选独立 ES 地址）";
    }

    @Override
    public String promptFragment() {
        return ElasticsearchToolDocumentation.aggregatePromptFragment();
    }

    @Override
    public String toolMenuBrief() {
        return """
                - es_indices: 列出索引（可选 cluster=skywalking）
                - es_search: DSL 搜索
                - es_count: 文档计数
                - es_aggregation: terms/date_histogram 等聚合
                """;
    }

    @Override
    public Set<String> toolNames() {
        return SkillToolHelp.toolNamesWithHelp(DATA_TOOLS, this);
    }

    @Override
    public String documentationForDataTool(String dataToolName) {
        return ElasticsearchToolDocumentation.docFor(dataToolName);
    }

    @Override
    public ToolResult execute(String toolName, Map<String, Object> args) {
        ToolResult help = SkillToolHelp.tryExecute(this, toolName, args);
        if (help != null) {
            return help;
        }
        String cluster = clusterArg(args);
        return switch (toolName) {
            case "es_indices" -> toolkit.listIndices(cluster);
            case "es_search" -> toolkit.search(cluster, stringArg(args, "index"), stringArg(args, "query"));
            case "es_count" -> toolkit.count(cluster, stringArg(args, "index"), stringArg(args, "query"));
            case "es_aggregation" -> toolkit.aggregate(cluster, stringArg(args, "index"), stringArg(args, "body"));
            default -> ToolResult.error("unknown tool: " + toolName);
        };
    }

    private static String clusterArg(Map<String, Object> args) {
        String c = stringArg(args, "cluster");
        return c.isBlank() ? "logs" : c;
    }

    private static String stringArg(Map<String, Object> args, String key) {
        if (args == null || !args.containsKey(key) || args.get(key) == null) {
            return "";
        }
        return String.valueOf(args.get(key));
    }
}
