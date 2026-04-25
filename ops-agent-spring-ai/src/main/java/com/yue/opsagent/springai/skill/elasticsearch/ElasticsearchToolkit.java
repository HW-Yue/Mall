package com.yue.opsagent.springai.skill.elasticsearch;

import com.yue.opsagent.springai.config.OpsAiProperties;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Thin HTTP client for Elasticsearch REST API (no MCP).
 * <p>
 * 支持两套地址：应用日志 ES 与（可选）SkyWalking OAP 专用 ES；后者未配置时与前者共用同一 {@link RestClient}。
 */
@Component
public class ElasticsearchToolkit {

    private final RestClient logsClient;
    private final RestClient skywalkingClient;

    public ElasticsearchToolkit(OpsAiProperties props) {
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
        String bodyIn = (jsonQuery == null || jsonQuery.isBlank())
                ? "{\"query\":{\"match_all\":{}}}"
                : jsonQuery;
        try {
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
                ? "{\"size\":0,\"aggs\":{\"sample\":{\"terms\":{\"field\":\"_id\",\"size\":1}}}}"
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
        String bodyIn = (jsonQuery == null || jsonQuery.isBlank())
                ? "{\"query\":{\"match_all\":{}},\"size\":10}"
                : jsonQuery;
        try {
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
}
