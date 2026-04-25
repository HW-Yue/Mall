package com.yue.opsagent.springai.skill.mysql;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class MysqlSkillRegistry implements OpsSkillRegistry {

    public static final String SKILL_NAME = "mysql_inspect";

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
        return "MySQL 只读：会话、状态、锁、慢查询样例";
    }

    @Override
    public String promptFragment() {
        return """
                - mysql_processlist: 无参
                - mysql_status: pattern(string 可选，传给 SHOW GLOBAL STATUS LIKE)
                - mysql_locks: performance_schema.metadata_locks 样例
                - mysql_slow_query: mysql.slow_log 最近行（需开启 slow_log 表）
                """;
    }

    @Override
    public String toolMenuBrief() {
        return """
                - mysql_processlist: 当前连接
                - mysql_status: 全局状态
                - mysql_locks: 元数据锁
                - mysql_slow_query: 慢查询表样例
                """;
    }

    @Override
    public String toolSpecification(String toolName) {
        return switch (toolName) {
            case "mysql_processlist" -> "mysql_processlist: args {}";
            case "mysql_status" -> "mysql_status: pattern 可选";
            case "mysql_locks" -> "mysql_locks: args {}";
            case "mysql_slow_query" -> "mysql_slow_query: args {}";
            default -> OpsSkillRegistry.super.toolSpecification(toolName);
        };
    }

    @Override
    public Set<String> toolNames() {
        return Set.of("mysql_processlist", "mysql_status", "mysql_locks", "mysql_slow_query");
    }

    @Override
    public ToolResult execute(String toolName, Map<String, Object> args) {
        Map<String, Object> a = args == null ? Map.of() : args;
        return switch (toolName) {
            case "mysql_processlist" -> toolkit.processlist();
            case "mysql_status" -> toolkit.globalStatusLike(str(a, "pattern"));
            case "mysql_locks" -> toolkit.metadataLocks();
            case "mysql_slow_query" -> toolkit.slowQuerySample();
            default -> ToolResult.error("unknown tool: " + toolName);
        };
    }

    private static String str(Map<String, Object> a, String key) {
        Object v = a.get(key);
        return v == null ? "" : String.valueOf(v);
    }
}
