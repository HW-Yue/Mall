package com.yue.opsagent.springai.domain.alert;

import java.util.HashMap;
import java.util.Map;

public final class AlertPlaceholderResolver {

    private AlertPlaceholderResolver() {}

    public static Map<String, String> flattenLabels(AlertEvent event) {
        return flatten(event, EnrichedAlertContext.empty());
    }

    public static Map<String, String> flatten(AlertEvent event, EnrichedAlertContext enrichment) {
        Map<String, String> m = new HashMap<>();
        if (event.labels() != null) {
            m.putAll(event.labels());
        }
        m.put("alertname", nullToEmpty(event.alertname()));
        m.put("severity", nullToEmpty(event.severity()));
        m.put("application", nullToEmpty(event.application()));
        if (enrichment != null) {
            putIfNotBlank(m, "primaryService", enrichment.primaryService());
            putIfNotBlank(m, "candidateServices", String.join(",", enrichment.candidateServices()));
            putIfNotBlank(m, "resource", enrichment.resource());
            putIfNotBlank(m, "topic", enrichment.topic());
            putIfNotBlank(m, "consumerGroup", enrichment.consumerGroup());
            putIfNotBlank(m, "table", enrichment.table());
            putIfNotBlank(m, "database", enrichment.database());
            putIfNotBlank(m, "pool", enrichment.pool());
            putIfNotBlank(m, "serviceReason", enrichment.serviceReason());
            if (enrichment.resolvedLabels() != null) {
                enrichment.resolvedLabels().forEach((k, v) -> putIfNotBlank(m, k, v));
            }
        }
        return m;
    }

    public static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public static Map<String, Object> resolveArgs(Map<String, Object> template, Map<String, String> labels) {
        if (template == null || template.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> resolved = new HashMap<>();
        for (var e : template.entrySet()) {
            resolved.put(e.getKey(), resolveValue(e.getValue(), labels));
        }
        return resolved;
    }

    public static String substituteTemplate(String s, Map<String, String> labels) {
        if (s == null) {
            return "";
        }
        String r = s;
        for (var e : labels.entrySet()) {
            r = r.replace("${" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return r;
    }

    private static Object resolveValue(Object v, Map<String, String> labels) {
        if (v instanceof String str) {
            return substituteTemplate(str, labels);
        }
        return v;
    }

    private static void putIfNotBlank(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }
}
