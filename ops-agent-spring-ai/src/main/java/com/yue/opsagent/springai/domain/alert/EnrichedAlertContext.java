package com.yue.opsagent.springai.domain.alert;

import java.util.List;
import java.util.Map;

public record EnrichedAlertContext(
        String primaryService,
        List<String> candidateServices,
        String resource,
        String topic,
        String consumerGroup,
        String table,
        String database,
        String pool,
        String serviceReason,
        Map<String, String> resolvedLabels,
        Map<String, Object> evidence
) {

    public static EnrichedAlertContext empty() {
        return new EnrichedAlertContext(
                "",
                List.of(),
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                Map.of(),
                Map.of());
    }
}
