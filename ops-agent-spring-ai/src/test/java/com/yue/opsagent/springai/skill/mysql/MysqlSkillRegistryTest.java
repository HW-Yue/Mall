package com.yue.opsagent.springai.skill.mysql;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MysqlSkillRegistryTest {

    @Test
    void routesExplainSqlToToolkit() {
        MysqlToolkit toolkit = mock(MysqlToolkit.class);
        MysqlSkillRegistry registry = new MysqlSkillRegistry(toolkit);
        when(toolkit.explainSql("select 1")).thenReturn(ToolResult.ok("explain_json", Map.of("plan", "{}")));

        ToolResult result = registry.execute("mysql_explain_sql", Map.of("sql", "select 1"));

        verify(toolkit).explainSql("select 1");
        assertThat(result).isInstanceOf(ToolResult.Ok.class);
        assertThat(registry.toolNames()).contains("mysql_explain_sql");
        assertThat(registry.documentationForDataTool("mysql_explain_sql")).contains("sql");
        assertThat(registry.documentationForDataTool("mysql_explain_sql")).contains("必填");
        assertThat(registry.toolSpecification("mysql_explain_sql")).contains("EXPLAIN");
    }

    @Test
    void helpToolReturnsDocumentation() {
        MysqlToolkit toolkit = mock(MysqlToolkit.class);
        MysqlSkillRegistry registry = new MysqlSkillRegistry(toolkit);

        ToolResult result =
                registry.execute(OpsSkillRegistry.DEFAULT_HELP_TOOL_NAME, Map.of(OpsSkillRegistry.HELP_ARG_TOOL, "mysql_processlist"));

        assertThat(result).isInstanceOf(ToolResult.Ok.class);
        assertThat(result.toMap().get("data").toString()).contains("连接");
    }

    @Test
    void helpToolRejectsUnknownTarget() {
        MysqlToolkit toolkit = mock(MysqlToolkit.class);
        MysqlSkillRegistry registry = new MysqlSkillRegistry(toolkit);

        ToolResult result =
                registry.execute(OpsSkillRegistry.DEFAULT_HELP_TOOL_NAME, Map.of(OpsSkillRegistry.HELP_ARG_TOOL, "nonexistent"));

        assertThat(result).isInstanceOf(ToolResult.Error.class);
    }

    @Test
    void normalizeExplainSqlOnlyAllowsSingleSelectStatements() {
        assertThat(MysqlToolkit.normalizeExplainSql(" select 1 ")).isEqualTo("select 1");
        assertThat(MysqlToolkit.normalizeExplainSql("with t as (select 1) select * from t")).isEqualTo("with t as (select 1) select * from t");
        assertThat(MysqlToolkit.normalizeExplainSql("update t set a = 1")).isNull();
        assertThat(MysqlToolkit.normalizeExplainSql("select 1; delete from t")).isNull();
        assertThat(MysqlToolkit.normalizeExplainSql("")).isNull();
    }
}
