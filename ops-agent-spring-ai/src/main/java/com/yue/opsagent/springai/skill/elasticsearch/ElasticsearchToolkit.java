package com.yue.opsagent.springai.skill.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Thin HTTP client for Elasticsearch REST API (no MCP).
 * <p>
 * 支持两套地址：应用日志 ES 与（可选）SkyWalking OAP 专用 ES；后者未配置时与前者共用同一 {@link RestClient}。
 */
@Component
public class ElasticsearchToolkit {

    private static final String DEFAULT_SEARCH_BODY = "{\"query\":{\"match_all\":{}},\"size\":10}";
    private static final String DEFAULT_COUNT_BODY = "{\"query\":{\"match_all\":{}}}";
    private static final String DEFAULT_AGG_BODY = "{\"size\":0,\"aggs\":{\"sample\":{\"terms\":{\"field\":\"_id\",\"size\":1}}}}";
    private static final String DEFAULT_LOG_INDEX = "nexus-*";
    private static final String DEFAULT_LOOKBACK = "1h";
    private static final int DEFAULT_ERROR_SAMPLE_SIZE = 10;
    private static final int MAX_ERROR_SAMPLE_SIZE = 20;
    private static final Pattern LOOKBACK_PATTERN = Pattern.compile("^\\d+[smhd]$");
    private static final Set<String> SEARCH_BODY_KEYS = Set.of(
            "query", "sort", "aggs", "_source", "from", "size", "track_total_hits",
            "highlight", "fields", "runtime_mappings", "pit", "search_after");
    private static final Set<String> QUERY_CLAUSE_KEYS = Set.of(
            "bool", "term", "terms", "range", "match", "multi_match", "query_string",
            "simple_query_string", "exists", "wildcard", "prefix", "regexp", "ids",
            "match_phrase", "match_phrase_prefix", "dis_max", "constant_score");
    private static final List<String> DEFAULT_ERROR_KEYWORDS = List.of(
            "error", "exception", "failed", "timeout", "refused", "deadlock");

    private final RestClient logsClient;
    private final RestClient skywalkingClient;
    private final ObjectMapper objectMapper;

    public ElasticsearchToolkit(OpsAiProperties props, ObjectMapper objectMapper) {
        var es = props.getElasticsearch();
        this.logsClient = buildClient(es.getBaseUrl(), es.getUsername(), es.getPassword());
        var sw = es.getSkywalking();
        String swUrl = sw != null ? nullToEmpty(sw.getBaseUrl()) : "";
        if (swUrl.isBlank()) {
            this.skywalkingClient = this.logsClient;
        } else {
            String swUser = sw != null ? nullToEmpty(sw.getUsername()) : "";
            String swPass = sw != null ? nullToEmpty(sw.getPassword()) : "";
            if (swUser.isBlank()) {
                swUser = nullToEmpty(es.getUsername());
                swPass = nullToEmpty(es.getPassword());
            }
            this.skywalkingClient = buildClient(swUrl, swUser, swPass);
        }
        this.objectMapper = objectMapper;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static RestClient buildClient(String baseUrl, String username, String password) {
        var builder = RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl));
        if (username != null && !username.isBlank()) {
            String token = Base64.getEncoder().encodeToString(
                    (username + ":" + (password == null ? "" : password)).getBytes(StandardCharsets.UTF_8));
            builder.defaultHeader("Authorization", "Basic " + token);
        }
        return builder.build();
    }

    private static RestClient clientFor(String cluster, RestClient logs, RestClient sw) {
        if (cluster == null || cluster.isBlank() || "logs".equalsIgnoreCase(cluster)) {
            return logs;
        }
        if ("skywalking".equalsIgnoreCase(cluster) || "sw".equalsIgnoreCase(cluster)) {
            return sw;
        }
        return null;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:9200";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public ToolResult listIndices() {
        return listIndices("logs");
    }

    /**
     * @param cluster {@code logs}（默认）或 {@code skywalking}/{@code sw}
     */
    public ToolResult listIndices(String cluster) {
        RestClient c = clientFor(cluster, logsClient, skywalkingClient);
        if (c == null) {
            return ToolResult.error("未知 cluster: " + cluster + "，请用 logs 或 skywalking");
        }
        try {
            String body = c.get()
                    .uri("/_cat/indices?format=json")
                    .retrieve()
                    .body(String.class);
            boolean askedSw = "skywalking".equalsIgnoreCase(cluster) || "sw".equalsIgnoreCase(cluster);
            return ToolResult.ok("索引列表 (" + (askedSw ? "skywalking" : "logs") + ")", body);
        } catch (RestClientException ex) {
            return ToolResult.error("ES list indices 失败: " + ex.getMessage());
        }
    }

    /**
     * @param index index pattern or name
     * @param jsonQuery request body JSON (query DSL), e.g. {@code {"query":{"match_all":{}}}}
     */
    public ToolResult count(String index, String jsonQuery) {
        return count("logs", index, jsonQuery);
    }

    public ToolResult count(String cluster, String index, String jsonQuery) {
        if (index == null || index.isBlank()) {
            return ToolResult.error("index 不能为空");
        }
        RestClient c = clientFor(cluster, logsClient, skywalkingClient);
        if (c == null) {
            return ToolResult.error("未知 cluster: " + cluster + "，请用 logs 或 skywalking");
        }
        try {
            String bodyIn = normalizeCountBody(jsonQuery);
            String path = "/" + index.trim() + "/_count";
            String body = c.post()
                    .uri(URI.create(path))
                    .header("Content-Type", "application/json")
                    .body(bodyIn)
                    .retrieve()
                    .body(String.class);
            return ToolResult.ok("计数结果", body);
        } catch (RestClientException ex) {
            return ToolResult.error("ES count 失败: " + ex.getMessage());
        }
    }

    /**
     * @param jsonBody 完整 search body，需包含 aggs（可含 size:0）
     */
    public ToolResult aggregate(String index, String jsonBody) {
        return aggregate("logs", index, jsonBody);
    }

    public ToolResult aggregate(String cluster, String index, String jsonBody) {
        if (index == null || index.isBlank()) {
            return ToolResult.error("index 不能为空");
        }
        RestClient c = clientFor(cluster, logsClient, skywalkingClient);
        if (c == null) {
            return ToolResult.error("未知 cluster: " + cluster + "，请用 logs 或 skywalking");
        }
        String bodyIn = (jsonBody == null || jsonBody.isBlank())
                ? DEFAULT_AGG_BODY
                : jsonBody;
        try {
            String path = "/" + index.trim() + "/_search";
            String body = c.post()
                    .uri(URI.create(path))
                    .header("Content-Type", "application/json")
                    .body(bodyIn)
                    .retrieve()
                    .body(String.class);
            return ToolResult.ok("聚合结果摘要", summarizeHits(body));
        } catch (RestClientException ex) {
            return ToolResult.error("ES aggregation 失败: " + ex.getMessage());
        }
    }

    /**
     * 以 logs 集群发起 _search，返回原始响应 JSON，供需要完整 hits 的调用方使用。
     */
    public String searchRawJson(String index, String jsonQuery) {
        if (index == null || index.isBlank()) {
            throw new IllegalArgumentException("index 不能为空");
        }
        String bodyIn = (jsonQuery == null || jsonQuery.isBlank())
                ? DEFAULT_SEARCH_BODY
                : normalizeSearchBody(jsonQuery);
        String path = "/" + index.trim() + "/_search";
        return logsClient.post()
                .uri(URI.create(path))
                .header("Content-Type", "application/json")
                .body(bodyIn)
                .retrieve()
                .body(String.class);
    }

    public ToolResult search(String index, String jsonQuery) {
        return search("logs", index, jsonQuery);
    }

    public ToolResult search(String cluster, String index, String jsonQuery) {
        if (index == null || index.isBlank()) {
            return ToolResult.error("index 不能为空");
        }
        RestClient c = clientFor(cluster, logsClient, skywalkingClient);
        if (c == null) {
            return ToolResult.error("未知 cluster: " + cluster + "，请用 logs 或 skywalking");
        }
        try {
            String bodyIn = normalizeSearchBody(jsonQuery);
            String path = "/" + index.trim() + "/_search";
            String body = c.post()
                    .uri(URI.create(path))
                    .header("Content-Type", "application/json")
                    .body(bodyIn)
                    .retrieve()
                    .body(String.class);
            return ToolResult.ok("搜索命中摘要", summarizeHits(body));
        } catch (RestClientException ex) {
            return ToolResult.error("ES search 失败: " + ex.getMessage());
        }
    }

    public ToolResult searchServiceErrors(
            String cluster,
            String index,
            String service,
            String application,
            String lookback,
            int size,
            List<String> keywords) {
        String normalizedService = trim(service);
        String normalizedApplication = trim(application);
        if (normalizedService.isBlank() && normalizedApplication.isBlank()) {
            return ToolResult.error("es_search_service_errors: service 或 application 至少提供一个");
        }
        String normalizedIndex = trim(index).isBlank() ? DEFAULT_LOG_INDEX : trim(index);
        RestClient c = clientFor(cluster, logsClient, skywalkingClient);
        if (c == null) {
            return ToolResult.error("未知 cluster: " + cluster + "，请用 logs 或 skywalking");
        }
        String normalizedLookback = normalizeLookback(lookback);
        int normalizedSize = normalizeErrorSampleSize(size);
        List<String> normalizedKeywords = normalizeKeywords(keywords);
        String bodyIn = buildServiceErrorSearchBody(
                normalizedService,
                normalizedApplication,
                normalizedLookback,
                normalizedSize,
                normalizedKeywords);
        try {
            String path = "/" + normalizedIndex + "/_search";
            String body = c.post()
                    .uri(URI.create(path))
                    .header("Content-Type", "application/json")
                    .body(bodyIn)
                    .retrieve()
                    .body(String.class);
            return ToolResult.ok("服务错误日志摘要", summarizeServiceErrorHits(
                    body,
                    cluster == null || cluster.isBlank() ? "logs" : cluster,
                    normalizedIndex,
                    normalizedService,
                    normalizedApplication,
                    normalizedLookback,
                    normalizedKeywords,
                    normalizedSize));
        } catch (RestClientException ex) {
            return ToolResult.error("ES service error search 失败: " + ex.getMessage());
        }
    }

    /** Return raw JSON or a short preview for the model. */
    private Object summarizeHits(String responseJson) {
        if (responseJson == null) {
            return "";
        }
        int max = 8000;
        if (responseJson.length() <= max) {
            return responseJson;
        }
        return Map.of(
                "truncated", true,
                "preview", responseJson.substring(0, max),
                "totalChars", responseJson.length()
        );
    }

    String normalizeSearchBody(String jsonQuery) {
        if (jsonQuery == null || jsonQuery.isBlank()) {
            return DEFAULT_SEARCH_BODY;
        }
        ObjectNode body = parseSearchObject(jsonQuery, "search query");
        if (!hasSearchBodyKey(body) && isLikelyQueryClause(body)) {
            ObjectNode wrapped = objectMapper.createObjectNode();
            wrapped.set("query", body);
            wrapped.put("size", DEFAULT_ERROR_SAMPLE_SIZE);
            return writeJson(wrapped);
        }
        if (!body.has("size")) {
            body.put("size", DEFAULT_ERROR_SAMPLE_SIZE);
        }
        return writeJson(body);
    }

    String normalizeCountBody(String jsonQuery) {
        if (jsonQuery == null || jsonQuery.isBlank()) {
            return DEFAULT_COUNT_BODY;
        }
        ObjectNode body = parseSearchObject(jsonQuery, "count query");
        if (!hasSearchBodyKey(body) && isLikelyQueryClause(body)) {
            ObjectNode wrapped = objectMapper.createObjectNode();
            wrapped.set("query", body);
            return writeJson(wrapped);
        }
        return writeJson(body);
    }

    static String normalizeLookback(String lookback) {
        String value = trim(lookback);
        return LOOKBACK_PATTERN.matcher(value).matches() ? value : DEFAULT_LOOKBACK;
    }

    static int normalizeErrorSampleSize(int size) {
        if (size <= 0) {
            return DEFAULT_ERROR_SAMPLE_SIZE;
        }
        return Math.min(size, MAX_ERROR_SAMPLE_SIZE);
    }

    static List<String> normalizeKeywords(List<String> keywords) {
        List<String> values = keywords == null ? List.of() : keywords.stream()
                .map(ElasticsearchToolkit::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        return values.isEmpty() ? DEFAULT_ERROR_KEYWORDS : values;
    }

    private String buildServiceErrorSearchBody(
            String service,
            String application,
            String lookback,
            int size,
            List<String> keywords) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("size", size);
        ArrayNode sort = body.putArray("sort");
        ObjectNode timestampSort = sort.addObject();
        timestampSort.putObject("@timestamp")
                .put("order", "desc")
                .put("unmapped_type", "date");

        ArrayNode source = body.putArray("_source");
        source.add("@timestamp");
        source.add("service");
        source.add("application");
        source.add("level");
        source.add("log.level");
        source.add("logger_name");
        source.add("message");
        source.add("formattedMessage");
        source.add("exception");
        source.add("throwable");
        source.add("stack_trace");

        ObjectNode query = body.putObject("query");
        ObjectNode bool = query.putObject("bool");
        ArrayNode filter = bool.putArray("filter");
        filter.add(serviceMatchClause(service, application));
        filter.addObject()
                .putObject("range")
                .putObject("@timestamp")
                .put("gte", "now-" + lookback);

        ObjectNode keywordQuery = bool.putArray("must").addObject();
        ObjectNode simpleQueryString = keywordQuery.putObject("simple_query_string");
        simpleQueryString.put("query", String.join(" | ", keywords));
        ArrayNode fields = simpleQueryString.putArray("fields");
        fields.add("message");
        fields.add("formattedMessage");
        fields.add("exception");
        fields.add("throwable");
        fields.add("stack_trace");
        simpleQueryString.put("default_operator", "or");

        return writeJson(body);
    }

    private ObjectNode serviceMatchClause(String service, String application) {
        LinkedHashSet<String> serviceValues = new LinkedHashSet<>();
        LinkedHashSet<String> applicationValues = new LinkedHashSet<>();
        if (!service.isBlank()) {
            serviceValues.add(service);
            applicationValues.add(service);
        }
        if (!application.isBlank()) {
            serviceValues.add(application);
            applicationValues.add(application);
        }
        ObjectNode bool = objectMapper.createObjectNode();
        ArrayNode should = bool.putObject("bool").putArray("should");
        serviceValues.forEach(value -> should.addObject()
                .putObject("term")
                .put("service.keyword", value));
        applicationValues.forEach(value -> should.addObject()
                .putObject("term")
                .put("application.keyword", value));
        bool.with("bool").put("minimum_should_match", 1);
        return bool;
    }

    private Map<String, Object> summarizeServiceErrorHits(
            String responseJson,
            String cluster,
            String index,
            String service,
            String application,
            String lookback,
            List<String> keywords,
            int size) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("cluster", cluster);
        summary.put("index", index);
        if (!service.isBlank()) {
            summary.put("service", service);
        }
        if (!application.isBlank()) {
            summary.put("application", application);
        }
        summary.put("lookback", lookback);
        summary.put("keywords", keywords);
        summary.put("size", size);
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            summary.put("totalHits", totalHits(root));
            summary.put("samples", samples(root));
            return summary;
        } catch (Exception ex) {
            summary.put("raw", summarizeHits(responseJson));
            summary.put("parseError", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            return summary;
        }
    }

    private long totalHits(JsonNode root) {
        JsonNode total = root.path("hits").path("total");
        if (total.isIntegralNumber()) {
            return total.longValue();
        }
        JsonNode value = total.path("value");
        return value.isIntegralNumber() ? value.longValue() : 0L;
    }

    private List<Map<String, Object>> samples(JsonNode root) {
        List<Map<String, Object>> out = new ArrayList<>();
        JsonNode hits = root.path("hits").path("hits");
        if (!hits.isArray()) {
            return out;
        }
        for (JsonNode hit : hits) {
            JsonNode source = hit.path("_source");
            Map<String, Object> item = new LinkedHashMap<>();
            putIfNotBlank(item, "timestamp", source.path("@timestamp").asText(""));
            putIfNotBlank(item, "service", source.path("service").asText(""));
            putIfNotBlank(item, "application", source.path("application").asText(""));
            String level = firstNonBlank(
                    source.path("level").asText(""),
                    source.path("log.level").asText(""),
                    source.path("log").path("level").asText(""));
            putIfNotBlank(item, "level", level);
            putIfNotBlank(item, "logger", source.path("logger_name").asText(""));
            putIfNotBlank(item, "message", truncate(firstNonBlank(
                    source.path("formattedMessage").asText(""),
                    source.path("message").asText("")), 240));
            putIfNotBlank(item, "exception", truncate(firstNonBlank(
                    source.path("exception").asText(""),
                    source.path("throwable").asText(""),
                    source.path("stack_trace").asText("")), 320));
            if (!item.isEmpty()) {
                out.add(item);
            }
        }
        return out;
    }

    private ObjectNode parseSearchObject(String jsonBody, String label) {
        try {
            JsonNode node = objectMapper.readTree(jsonBody);
            if (!(node instanceof ObjectNode objectNode)) {
                throw new IllegalArgumentException(label + " 必须是 JSON object");
            }
            return objectNode.deepCopy();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("ES " + label + " JSON 非法: " + e.getMessage(), e);
        }
    }

    private boolean hasSearchBodyKey(ObjectNode body) {
        for (String key : SEARCH_BODY_KEYS) {
            if (body.has(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLikelyQueryClause(ObjectNode body) {
        if (body.isEmpty()) {
            return false;
        }
        var fields = body.fieldNames();
        while (fields.hasNext()) {
            if (!QUERY_CLAUSE_KEYS.contains(fields.next())) {
                return false;
            }
        }
        return true;
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalArgumentException("ES 查询体序列化失败: " + e.getMessage(), e);
        }
    }

    private static void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value == null ? "" : value;
        }
        return value.substring(0, max) + "...";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
