package com.yue.opsagent.springai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.domain.opsroute.RouteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcOpsRunSummaryRepositoryTest {

    private JdbcOpsRunSummaryRepository repository;
    private OpsRunService service;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:ops_run_summary;MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP TABLE IF EXISTS ops_run_summary");
        jdbcTemplate.execute("""
                CREATE TABLE ops_run_summary (
                    run_id VARCHAR(64) PRIMARY KEY,
                    input_type VARCHAR(32),
                    status VARCHAR(32),
                    current_node VARCHAR(64),
                    event_count BIGINT NOT NULL,
                    first_event_type VARCHAR(64),
                    first_event_message VARCHAR(255),
                    last_event_type VARCHAR(64),
                    last_event_message VARCHAR(255),
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);
        repository = new JdbcOpsRunSummaryRepository(jdbcTemplate, "ops_run_summary");
        service = new OpsRunService(new ObjectMapper(), repository);
    }

    @Test
    void upsertPersistsAndUpdatesSummary() {
        var session = service.create(RouteRequest.text("hello"));
        service.node(session.runId(), "AiSopMatch", "命中规则");
        service.complete(session.runId(), "End", "完成", java.util.Map.of());

        List<?> rows = repository.findRecentRuns(10);

        assertThat(rows).hasSize(1);
        var summary = repository.findRecentRuns(10).getFirst();
        assertThat(summary.runId()).isEqualTo(session.runId());
        assertThat(summary.eventCount()).isEqualTo(3);
        assertThat(summary.currentNode()).isEqualTo("End");
        assertThat(summary.firstEventType()).isEqualTo("start");
        assertThat(summary.lastEventType()).isEqualTo("end");
        assertThat(summary.status().name()).isEqualTo("COMPLETED");
        assertThat(summary.source()).isEqualTo("db");
    }
}
