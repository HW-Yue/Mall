package com.yue.opsagent.springai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.skill.elasticsearch.ElasticsearchToolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从 ES（logstash 索引）查询 ops-agent run 历史。
 * 数据来源：{@link OpsRunService#addEvent} 通过 logstash-logback-encoder 写入的结构化日志。
 */
@Service
public class OpsRunHistoryService {

    private static final Logger log = LoggerFactory.getLogger(OpsRunHistoryService.class);

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ElasticsearchToolkit elasticsearch;
    private final ObjectMapper objectMapper;
    private final String indexPattern;
    private final String serviceName;

    public OpsRunHistoryService(
            ElasticsearchToolkit elasticsearch,
            ObjectMapper objectMapper,
            @Value("${ops.history.es.index:logstash-*}") String indexPattern,
            @Value("${spring.application.name:yue-ops-agent}") String serviceName) {
        this.elasticsearch = elasticsearch;
        this.objectMapper = objectMapper;
        this.indexPattern = indexPattern;
        this.serviceName = serviceName;
    }

    /** 按 runId 查询单次运行的完整事件时间线，按 @timestamp 升序。 */
    public List<Map<String, Object>> timeline(String runId) {
        if (runId == null || runId.isBlank()) {
            return List.of();
        }
        String query = "{"
                + "\"size\":500,"
                + "\"sort\":[{\"@timestamp\":\"asc\"}],"
                + "\"query\":{\"term\":{\"runId.keyword\":\"" + escape(runId) + "\"}},"
                + "\"_source\":[\"@timestamp\",\"runId\",\"eventType\",\"node\",\"eventMessage\",\"data\",\"service\"]"
                + "}";
        try {
            String json = elasticsearch.searchRawJson(indexPattern, query);
            return parseHits(json);
        } catch (Exception ex) {
            log.warn("[OpsRunHistory] timeline 查询失败 runId={} err={}", runId, ex.toString());
            return List.of();
        }
    }

    /**
     * 列出最近 size 个 run 的摘要，按最近一次事件时间倒序。
     * 通过 terms aggregation 在 runId.keyword 上分桶，并取每桶 top_hits 的 start 事件作为摘要。
     */
    public List<Map<String, Object>> recentRuns(int size) {
        int s = Math.max(1, Math.min(size, 200));
        String query = "{"
                + "\"size\":0,"
                + "\"query\":{\"bool\":{\"filter\":["
                + "{\"term\":{\"service.keyword\":\"" + escape(serviceName) + "\"}},"
                + "{\"exists\":{\"field\":\"runId\"}}"
                + "]}},"
                + "\"aggs\":{\"runs\":{"
                + "\"terms\":{\"field\":\"runId.keyword\",\"size\":" + s + ",\"order\":{\"latest\":\"desc\"}},"
                + "\"aggs\":{"
                + "\"latest\":{\"max\":{\"field\":\"@timestamp\"}},"
                + "\"first_event\":{\"top_hits\":{\"size\":1,\"sort\":[{\"@timestamp\":\"asc\"}],"
                + "\"_source\":[\"@timestamp\",\"runId\",\"eventType\",\"node\",\"eventMessage\",\"data\"]}},"
                + "\"last_event\":{\"top_hits\":{\"size\":1,\"sort\":[{\"@timestamp\":\"desc\"}],"
                + "\"_source\":[\"@timestamp\",\"runId\",\"eventType\",\"node\",\"eventMessage\",\"data\"]}}"
                + "}}}"
                + "}";
        try {
            String json = elasticsearch.searchRawJson(indexPattern, query);
            return parseRunBuckets(json);
        } catch (Exception ex) {
            log.warn("[OpsRunHistory] recentRuns 查询失败 err={}", ex.toString());
            return List.of();
        }
    }

    private List<Map<String, Object>> parseHits(String responseJson) {
        List<Map<String, Object>> events = new ArrayList<>();
        if (responseJson == null || responseJson.isBlank()) {
            return events;
        }
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode hits = root.path("hits").path("hits");
            if (!hits.isArray()) {
                return events;
            }
            for (JsonNode hit : hits) {
                JsonNode source = hit.path("_source");
                if (source.isMissingNode()) continue;
                events.add(objectMapper.convertValue(source, MAP_TYPE));
            }
        } catch (Exception ex) {
            log.warn("[OpsRunHistory] 解析 ES 响应失败 err={}", ex.toString());
        }
        return events;
    }

    private List<Map<String, Object>> parseRunBuckets(String responseJson) {
        List<Map<String, Object>> runs = new ArrayList<>();
        if (responseJson == null || responseJson.isBlank()) {
            return runs;
        }
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode buckets = root.path("aggregations").path("runs").path("buckets");
            if (!buckets.isArray()) {
                return runs;
            }
            for (JsonNode bucket : buckets) {
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("runId", bucket.path("key").asText());
                summary.put("eventCount", bucket.path("doc_count").asLong());
                summary.put("latest", bucket.path("latest").path("value_as_string").asText(
                        bucket.path("latest").path("value").asText()));
                JsonNode firstHit = bucket.path("first_event").path("hits").path("hits");
                JsonNode lastHit = bucket.path("last_event").path("hits").path("hits");
                if (firstHit.isArray() && firstHit.size() > 0) {
                    summary.put("firstEvent", objectMapper.convertValue(firstHit.get(0).path("_source"), MAP_TYPE));
                }
                if (lastHit.isArray() && lastHit.size() > 0) {
                    summary.put("lastEvent", objectMapper.convertValue(lastHit.get(0).path("_source"), MAP_TYPE));
                }
                runs.add(summary);
            }
        } catch (Exception ex) {
            log.warn("[OpsRunHistory] 解析 run buckets 失败 err={}", ex.toString());
        }
        return runs;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
