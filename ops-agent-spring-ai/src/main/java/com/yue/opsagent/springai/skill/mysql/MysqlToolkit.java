package com.yue.opsagent.springai.skill.mysql;

import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 只读 JDBC 诊断（请使用只读账号）。
 */
@Component
public class MysqlToolkit {

    private final OpsAiProperties props;

    public MysqlToolkit(OpsAiProperties props) {
        this.props = props;
    }

    private Connection connect() throws Exception {
        var m = props.getMysql();
        return DriverManager.getConnection(m.getJdbcUrl(), m.getUsername(), m.getPassword());
    }

    private ToolResult query(String title, String sql, int maxRows) {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.setMaxRows(maxRows);
            try (ResultSet rs = st.executeQuery(sql)) {
                return ToolResult.ok(title, toJsonRows(rs, maxRows));
            }
        } catch (Exception e) {
            return ToolResult.error(title + " 失败: " + e.getMessage());
        }
    }

    private static List<Map<String, Object>> toJsonRows(ResultSet rs, int maxRows) throws Exception {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();
        int n = 0;
        while (rs.next() && n < maxRows) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= cols; i++) {
                row.put(md.getColumnLabel(i), rs.getObject(i));
            }
            rows.add(row);
            n++;
        }
        return rows;
    }

    public ToolResult processlist() {
        return query("processlist", "SELECT * FROM information_schema.processlist ORDER BY TIME DESC LIMIT 80", 80);
    }

    public ToolResult globalStatusLike(String likePattern) {
        String p = (likePattern == null || likePattern.isBlank()) ? "%" : likePattern.replace('\'', ' ');
        String sql = "SHOW GLOBAL STATUS LIKE '" + p + "'";
        return query("status", sql, 500);
    }

    public ToolResult metadataLocks() {
        return query("metadata_locks",
                "SELECT * FROM performance_schema.metadata_locks WHERE OBJECT_TYPE IS NOT NULL LIMIT 100",
                100);
    }

    public ToolResult slowQuerySample() {
        return query("slow_log",
                "SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 20",
                20);
    }
}
