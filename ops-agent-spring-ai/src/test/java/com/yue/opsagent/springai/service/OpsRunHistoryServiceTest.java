package com.yue.opsagent.springai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.domain.opsroute.OpsRunStatus;
import com.yue.opsagent.springai.domain.opsroute.OpsRunSummary;
import com.yue.opsagent.springai.domain.opsroute.RouteInputType;
import com.yue.opsagent.springai.skill.elasticsearch.ElasticsearchToolkit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpsRunHistoryServiceTest {

    private static final String SAMPLE_HITS = """
            {
              "hits": {
                "hits": [
                  {"_source": {"@timestamp":"2026-05-07T10:00:00Z","runId":"r1","eventType":"start","node":"Root","eventMessage":"运行已创建","data":{"inputType":"TEXT"}}},
                  {"_source": {"@timestamp":"2026-05-07T10:00:01Z","runId":"r1","eventType":"tool_start","node":"metrics_ops","eventMessage":"调用工具 metrics_query","data":{"skill":"metrics_ops","tool":"metrics_query"}}}
                ]
              }
            }
            """;

    @Test
    void timelineQueriesByRunIdKeywordSortedAsc() {
        ElasticsearchToolkit toolkit = mock(ElasticsearchToolkit.class);
        when(toolkit.searchRawJson(eq("logstash-*"), anyString())).thenReturn(SAMPLE_HITS);

        OpsRunHistoryService service = new OpsRunHistoryService(
                toolkit, OpsRunSummaryRepository.noop(), new ObjectMapper(), "logstash-*", "yue-ops-agent");

        List<Map<String, Object>> events = service.timeline("r1");

        ArgumentCaptor<String> queryCap = ArgumentCaptor.forClass(String.class);
        verify(toolkit).searchRawJson(eq("logstash-*"), queryCap.capture());
        String dsl = queryCap.getValue();
        assertThat(dsl).contains("\"runId.keyword\":\"r1\"");
        assertThat(dsl).contains("\"@timestamp\":\"asc\"");

        assertThat(events).hasSize(2);
        assertThat(events.get(0)).containsEntry("eventType", "start");
        assertThat(events.get(0)).containsEntry("runId", "r1");
        assertThat(events.get(1)).containsEntry("eventType", "tool_start");
    }

    @Test
    void recentRunsReadsFromSummaryRepository() {
        ElasticsearchToolkit toolkit = mock(ElasticsearchToolkit.class);
        OpsRunSummaryRepository repository = mock(OpsRunSummaryRepository.class);
        when(repository.findRecentRuns(50)).thenReturn(List.of(new OpsRunSummary(
                "r1",
                RouteInputType.TEXT,
                OpsRunStatus.COMPLETED,
                "End",
                12,
                "start",
                "运行已创建",
                "end",
                "完成",
                Instant.parse("2026-05-07T10:00:00Z"),
                Instant.parse("2026-05-07T10:00:30Z"),
                "db"
        )));

        OpsRunHistoryService service = new OpsRunHistoryService(
                toolkit, repository, new ObjectMapper(), "logstash-*", "yue-ops-agent");

        List<OpsRunSummary> runs = service.recentRuns(50);

        verify(repository).findRecentRuns(50);

        assertThat(runs).hasSize(1);
        OpsRunSummary r = runs.getFirst();
        assertThat(r.runId()).isEqualTo("r1");
        assertThat(r.eventCount()).isEqualTo(12);
        assertThat(r.source()).isEqualTo("db");
    }

    @Test
    void timelineReturnsEmptyOnEsFailure() {
        ElasticsearchToolkit toolkit = mock(ElasticsearchToolkit.class);
        when(toolkit.searchRawJson(anyString(), anyString())).thenThrow(new RuntimeException("boom"));

        OpsRunHistoryService service = new OpsRunHistoryService(
                toolkit, OpsRunSummaryRepository.noop(), new ObjectMapper(), "logstash-*", "yue-ops-agent");

        assertThat(service.timeline("r1")).isEmpty();
    }
}
