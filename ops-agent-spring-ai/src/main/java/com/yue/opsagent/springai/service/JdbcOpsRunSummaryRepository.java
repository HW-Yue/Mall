package com.yue.opsagent.springai.service;

import com.yue.opsagent.springai.domain.opsroute.OpsRunEvent;
import com.yue.opsagent.springai.domain.opsroute.OpsRunSession;
import com.yue.opsagent.springai.domain.opsroute.OpsRunStatus;
import com.yue.opsagent.springai.domain.opsroute.OpsRunSummary;
import com.yue.opsagent.springai.domain.opsroute.RouteInputType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

public class JdbcOpsRunSummaryRepository implements OpsRunSummaryRepository {

    private static final int MAX_SIZE = 200;

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;

    public JdbcOpsRunSummaryRepository(JdbcTemplate jdbcTemplate, String tableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
    }

    @Override
    public void upsert(OpsRunSession session) {
        OpsRunSummary summary = toSummary(session, "db");
        int updated = jdbcTemplate.update("""
                UPDATE %s
                   SET input_type = ?,
                       status = ?,
                       current_node = ?,
                       event_count = ?,
                       first_event_type = ?,
                       first_event_message = ?,
                       last_event_type = ?,
                       last_event_message = ?,
                       created_at = ?,
                       updated_at = ?
                 WHERE run_id = ?
                """.formatted(tableName),
                summary.inputType() == null ? null : summary.inputType().name(),
                summary.status() == null ? null : summary.status().name(),
                summary.currentNode(),
                summary.eventCount(),
                summary.firstEventType(),
                summary.firstEventMessage(),
                summary.lastEventType(),
                summary.lastEventMessage(),
                toTimestamp(summary.createdAt()),
                toTimestamp(summary.updatedAt()),
                summary.runId());
        if (updated > 0) {
            return;
        }
        try {
            jdbcTemplate.update("""
                    INSERT INTO %s (
                        run_id, input_type, status, current_node, event_count,
                        first_event_type, first_event_message, last_event_type, last_event_message,
                        created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.formatted(tableName),
                    summary.runId(),
                    summary.inputType() == null ? null : summary.inputType().name(),
                    summary.status() == null ? null : summary.status().name(),
                    summary.currentNode(),
                    summary.eventCount(),
                    summary.firstEventType(),
                    summary.firstEventMessage(),
                    summary.lastEventType(),
                    summary.lastEventMessage(),
                    toTimestamp(summary.createdAt()),
                    toTimestamp(summary.updatedAt()));
        } catch (DuplicateKeyException e) {
            jdbcTemplate.update("""
                    UPDATE %s
                       SET input_type = ?,
                           status = ?,
                           current_node = ?,
                           event_count = ?,
                           first_event_type = ?,
                           first_event_message = ?,
                           last_event_type = ?,
                           last_event_message = ?,
                           created_at = ?,
                           updated_at = ?
                     WHERE run_id = ?
                    """.formatted(tableName),
                    summary.inputType() == null ? null : summary.inputType().name(),
                    summary.status() == null ? null : summary.status().name(),
                    summary.currentNode(),
                    summary.eventCount(),
                    summary.firstEventType(),
                    summary.firstEventMessage(),
                    summary.lastEventType(),
                    summary.lastEventMessage(),
                    toTimestamp(summary.createdAt()),
                    toTimestamp(summary.updatedAt()),
                    summary.runId());
        }
    }

    @Override
    public List<OpsRunSummary> findRecentRuns(int size) {
        int limit = Math.max(1, Math.min(size, MAX_SIZE));
        return jdbcTemplate.query("""
                SELECT run_id, input_type, status, current_node, event_count,
                       first_event_type, first_event_message, last_event_type, last_event_message,
                       created_at, updated_at
                  FROM %s
                 ORDER BY updated_at DESC
                 LIMIT ?
                """.formatted(tableName), this::mapRow, limit);
    }

    static OpsRunSummary toSummary(OpsRunSession session, String source) {
        List<OpsRunEvent> events = session.events() == null ? List.of() : session.events();
        OpsRunEvent first = events.isEmpty() ? null : events.getFirst();
        OpsRunEvent last = events.isEmpty() ? null : events.getLast();
        return new OpsRunSummary(
                session.runId(),
                session.inputType(),
                session.status(),
                session.currentNode(),
                events.size(),
                first == null ? "" : first.type(),
                first == null ? "" : first.message(),
                last == null ? "" : last.type(),
                last == null ? "" : last.message(),
                session.createdAt(),
                session.updatedAt(),
                source);
    }

    private OpsRunSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new OpsRunSummary(
                rs.getString("run_id"),
                enumValue(RouteInputType.class, rs.getString("input_type")),
                enumValue(OpsRunStatus.class, rs.getString("status")),
                rs.getString("current_node"),
                rs.getLong("event_count"),
                rs.getString("first_event_type"),
                rs.getString("first_event_message"),
                rs.getString("last_event_type"),
                rs.getString("last_event_message"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at")),
                "db");
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        return value == null || value.isBlank() ? null : Enum.valueOf(type, value);
    }
}
