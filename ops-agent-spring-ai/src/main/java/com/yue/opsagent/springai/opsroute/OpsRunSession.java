package com.yue.opsagent.springai.opsroute;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OpsRunSession(
        String runId,
        RouteInputType inputType,
        OpsRunStatus status,
        String currentNode,
        Instant createdAt,
        Instant updatedAt,
        List<OpsRunEvent> events
) {
    public Map<String, Object> toMap() {
        return Map.of(
                "runId", runId,
                "inputType", inputType,
                "status", status,
                "currentNode", currentNode == null ? "" : currentNode,
                "createdAt", createdAt,
                "updatedAt", updatedAt,
                "events", events == null ? List.of() : events);
    }
}
