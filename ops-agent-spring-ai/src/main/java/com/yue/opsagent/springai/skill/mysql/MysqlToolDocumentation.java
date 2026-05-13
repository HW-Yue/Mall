package com.yue.opsagent.springai.skill.mysql;

import java.util.Arrays;
import java.util.stream.Collectors;

enum MysqlToolDocumentation {

    mysql_processlist("""
            无参（args 可为 {}）。
            查看当前连接与会话类信息（processlist 语义）。"""),

    mysql_status("""
            pattern (string 可选)：传给 SHOW GLOBAL STATUS LIKE；空则返回常用全局状态摘要。"""),

    mysql_locks("""
            无参（args 可为 {}）。
            采样 performance_schema.metadata_locks。"""),

    mysql_slow_query("""
            无参（args 可为 {}）。
            读取 mysql.slow_log 最近样例行（需已开启 slow_log 且使用表存储）。"""),

    mysql_explain_sql("""
            sql (string 必填)：仅允许 SELECT / WITH ... SELECT；执行 EXPLAIN FORMAT=JSON。
            非只读或拆分多语句的 SQL 会被拒绝。""");

    private final String usage;

    MysqlToolDocumentation(String usage) {
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
        return Arrays.stream(values())
                .map(e -> "- " + e.name() + ":\n" + e.usage)
                .collect(Collectors.joining("\n\n"));
    }
}
