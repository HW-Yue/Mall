package com.yue.opsagent.springai.domain.opsroute;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record OpsRunSummary(
        String runId,
        RouteInputType inputType,
        OpsRunStatus status,
        String currentNode,
        long eventCount,
        String firstEventType,
        String firstEventMessage,
        String lastEventType,
        String lastEventMessage,
        Instant createdAt,
        Instant updatedAt,
        String source
) {

    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("runId", runId);
        out.put("inputType", inputType);
        out.put("status", status);
        out.put("currentNode", currentNode == null ? "" : currentNode);
        out.put("eventCount", eventCount);
        out.put("createdAt", createdAt);
        out.put("updatedAt", updatedAt);
        out.put("latest", updatedAt);
        out.put("source", source == null ? "" : source);
        out.put("firstEvent", eventMap(firstEventType, firstEventMessage));
        out.put("lastEvent", eventMap(lastEventType, lastEventMessage));
        return out;
    }

    private static Map<String, Object> eventMap(String eventType, String eventMessage) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", eventType == null ? "" : eventType);
        event.put("eventMessage", eventMessage == null ? "" : eventMessage);
        event.put("data", Map.of());
        return event;
    }
}
