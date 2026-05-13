package com.yue.opsagent.springai.skill.mysql;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.support.SkillToolHelp;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class MysqlSkillRegistry implements OpsSkillRegistry {

    public static final String SKILL_NAME = "mysql_inspect";

    private static final Set<String> DATA_TOOLS =
            Set.of("mysql_processlist", "mysql_status", "mysql_locks", "mysql_slow_query", "mysql_explain_sql");

    private final MysqlToolkit toolkit;

    public MysqlSkillRegistry(MysqlToolkit toolkit) {
        this.toolkit = toolkit;
    }

    @Override
    public String name() {
        return SKILL_NAME;
    }

    @Override
    public String description() {
        return "MySQL 只读：会话、状态、锁、慢查询样例、SELECT 执行计划";
    }

    @Override
    public String promptFragment() {
        return MysqlToolDocumentation.aggregatePromptFragment();
    }

    @Override
    public String toolMenuBrief() {
        return """
                - mysql_processlist: 当前连接
                - mysql_status: 全局状态
                - mysql_locks: 元数据锁
                - mysql_slow_query: 慢查询表样例
                - mysql_explain_sql: SELECT 执行计划(JSON)
                """;
    }

    @Override
    public Set<String> toolNames() {
        return SkillToolHelp.toolNamesWithHelp(DATA_TOOLS, this);
    }

    @Override
    public String documentationForDataTool(String dataToolName) {
        return MysqlToolDocumentation.docFor(dataToolName);
    }

    @Override
    public ToolResult execute(String toolName, Map<String, Object> args) {
        ToolResult help = SkillToolHelp.tryExecute(this, toolName, args);
        if (help != null) {
            return help;
        }
        Map<String, Object> a = args == null ? Map.of() : args;
        return switch (toolName) {
            case "mysql_processlist" -> toolkit.processlist();
            case "mysql_status" -> toolkit.globalStatusLike(str(a, "pattern"));
            case "mysql_locks" -> toolkit.metadataLocks();
            case "mysql_slow_query" -> toolkit.slowQuerySample();
            case "mysql_explain_sql" -> toolkit.explainSql(str(a, "sql"));
            default -> ToolResult.error("unknown tool: " + toolName);
        };
    }

    private static String str(Map<String, Object> a, String key) {
        Object v = a.get(key);
        return v == null ? "" : String.valueOf(v);
    }
}
