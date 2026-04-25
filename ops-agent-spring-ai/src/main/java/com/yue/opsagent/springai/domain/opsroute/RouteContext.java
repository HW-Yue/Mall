package com.yue.opsagent.springai.domain.opsroute;

import com.yue.opsagent.springai.domain.alert.AlertEvent;
import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;

import java.util.LinkedHashMap;
import java.util.Map;

public class RouteContext {

    private final String runId;
    private final RouteRequest request;
    private OpsAiProperties.Sop.Rule matchedRule;
    private String matchSource = "";
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    public RouteContext(String runId, RouteRequest request) {
        this.runId = runId;
        this.request = request;
    }

    public String runId() {
        return runId;
    }

    public RouteRequest request() {
        return request;
    }

    public AlertEvent alertEvent() {
        return request.alertEvent();
    }

    public OpsAiProperties.Sop.Rule matchedRule() {
        return matchedRule;
    }

    public void matchedRule(OpsAiProperties.Sop.Rule matchedRule, String matchSource) {
        this.matchedRule = matchedRule;
        this.matchSource = matchSource == null ? "" : matchSource;
    }

    public String matchSource() {
        return matchSource;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }
}
