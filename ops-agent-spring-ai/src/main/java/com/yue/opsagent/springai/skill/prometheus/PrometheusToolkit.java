package com.yue.opsagent.springai.skill.prometheus;

import com.yue.opsagent.springai.config.OpsAiProperties;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Instant PromQL over Prometheus HTTP API.
 */
@Component
public class PrometheusToolkit {

    private final RestClient client;
    private final String baseUrl;

    public PrometheusToolkit(OpsAiProperties props) {
        this.baseUrl = trimTrailingSlash(props.getPrometheus().getBaseUrl());
        this.client = RestClient.builder().baseUrl(this.baseUrl).build();
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:9090";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public ToolResult queryInstant(String promql) {
        if (promql == null || promql.isBlank()) {
            return ToolResult.error("promql 不能为空");
        }
        try {
            String uri = UriComponentsBuilder.fromPath("/api/v1/query")
                    .queryParam("query", promql)
                    .build()
                    .toUriString();
            String body = client.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
            return ToolResult.ok("Prometheus instant query", body);
        } catch (RestClientException ex) {
            return ToolResult.error("Prometheus query 失败: " + ex.getMessage());
        }
    }
}
