package com.yue.opsagent.springai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.skill.elasticsearch.ElasticsearchToolkit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

    private static final String SAMPLE_BUCKETS = """
            {
              "aggregations": {
                "runs": {
                  "buckets": [
                    {
                      "key":"r1","doc_count":12,
                      "latest":{"value":1.7e12,"value_as_string":"2026-05-07T10:00:30Z"},
                      "first_event":{"hits":{"hits":[{"_source":{"@timestamp":"2026-05-07T10:00:00Z","eventType":"start","node":"Root","eventMessage":"运行已创建","data":{}}}]}},
                      "last_event":{"hits":{"hits":[{"_source":{"@timestamp":"2026-05-07T10:00:30Z","eventType":"end","node":"End","eventMessage":"完成","data":{}}}]}}
                    }
                  ]
                }
              }
            }
            """;

    @Test
    void timelineQueriesByRunIdKeywordSortedAsc() {
        ElasticsearchToolkit toolkit = mock(ElasticsearchToolkit.class);
        when(toolkit.searchRawJson(eq("logstash-*"), anyString())).thenReturn(SAMPLE_HITS);

        OpsRunHistoryService service = new OpsRunHistoryService(
                toolkit, new ObjectMapper(), "logstash-*", "yue-ops-agent");

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
    void recentRunsParsesBucketsIntoSummaries() {
        ElasticsearchToolkit toolkit = mock(ElasticsearchToolkit.class);
        when(toolkit.searchRawJson(eq("logstash-*"), anyString())).thenReturn(SAMPLE_BUCKETS);

        OpsRunHistoryService service = new OpsRunHistoryService(
                toolkit, new ObjectMapper(), "logstash-*", "yue-ops-agent");

        List<Map<String, Object>> runs = service.recentRuns(50);

        ArgumentCaptor<String> queryCap = ArgumentCaptor.forClass(String.class);
        verify(toolkit).searchRawJson(eq("logstash-*"), queryCap.capture());
        String dsl = queryCap.getValue();
        assertThat(dsl).contains("\"service.keyword\":\"yue-ops-agent\"");
        assertThat(dsl).contains("\"field\":\"runId.keyword\"");
        assertThat(dsl).contains("\"size\":50");

        assertThat(runs).hasSize(1);
        Map<String, Object> r = runs.get(0);
        assertThat(r).containsEntry("runId", "r1");
        assertThat(r).containsEntry("eventCount", 12L);
        assertThat(r.get("firstEvent")).isInstanceOf(Map.class);
        assertThat(r.get("lastEvent")).isInstanceOf(Map.class);
    }

    @Test
    void timelineReturnsEmptyOnEsFailure() {
        ElasticsearchToolkit toolkit = mock(ElasticsearchToolkit.class);
        when(toolkit.searchRawJson(anyString(), anyString())).thenThrow(new RuntimeException("boom"));

        OpsRunHistoryService service = new OpsRunHistoryService(
                toolkit, new ObjectMapper(), "logstash-*", "yue-ops-agent");

        assertThat(service.timeline("r1")).isEmpty();
    }
}
