package com.yue.opsagent.springai.opsroute;

import java.time.Instant;
import java.util.Map;

public record OpsRunEvent(
        String type,
        String node,
        String message,
        Instant at,
        Map<String, Object> data
) {
    public OpsRunEvent {
        at = at == null ? Instant.now() : at;
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    public static OpsRunEvent of(String type, String node, String message, Map<String, Object> data) {
        return new OpsRunEvent(type, node, message, Instant.now(), data);
    }
}
