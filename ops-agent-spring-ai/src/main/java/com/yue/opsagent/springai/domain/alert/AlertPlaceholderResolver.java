package com.yue.opsagent.springai.domain.alert;

import java.util.HashMap;
import java.util.Map;

public final class AlertPlaceholderResolver {

    private AlertPlaceholderResolver() {}

    public static Map<String, String> flattenLabels(AlertEvent event) {
        Map<String, String> m = new HashMap<>();
        if (event.labels() != null) {
            m.putAll(event.labels());
        }
        m.put("alertname", nullToEmpty(event.alertname()));
        m.put("severity", nullToEmpty(event.severity()));
        m.put("application", nullToEmpty(event.application()));
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
}
