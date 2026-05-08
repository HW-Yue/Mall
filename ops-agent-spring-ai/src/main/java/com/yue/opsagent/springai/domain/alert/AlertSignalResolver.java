package com.yue.opsagent.springai.domain.alert;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class AlertSignalResolver {

    private final OpsKnowledgeCatalog catalog;

    public AlertSignalResolver(OpsKnowledgeCatalog catalog) {
        this.catalog = catalog;
    }

    public AlertSignals resolve(AlertEvent event) {
        Map<String, String> labels = event.labels() == null ? Map.of() : event.labels();
        Map<String, String> annotations = event.annotations() == null ? Map.of() : event.annotations();
        Set<String> texts = new LinkedHashSet<>();
        labels.values().forEach(v -> addText(texts, v));
        annotations.values().forEach(v -> addText(texts, v));
        addText(texts, event.application());
        addText(texts, labels.get("resource"));
        addText(texts, labels.get("topic"));
        addText(texts, firstNonBlank(labels.get("consumerGroup"), labels.get("group"), labels.get("consumer_group")));
        return new AlertSignals(
                collectApplications(event, labels, texts),
                collectResources(labels, texts),
                collectFromTexts(labels.get("topic"), catalog.knownTopics(), texts),
                collectFromTexts(firstNonBlank(labels.get("consumerGroup"), labels.get("group"), labels.get("consumer_group")),
                        catalog.knownConsumerGroups(), texts),
                collectFromTexts(firstNonBlank(labels.get("table"), labels.get("tableName"), labels.get("object")),
                        catalog.knownTables(), texts),
                collectFromTexts(firstNonBlank(labels.get("db"), labels.get("database"), labels.get("schema")),
                        catalog.knownDatabases(), texts),
                collectFromTexts(firstNonBlank(labels.get("pool"), labels.get("poolName")), catalog.knownPools(), texts),
                List.copyOf(texts));
    }

    private List<String> collectApplications(AlertEvent event, Map<String, String> labels, Set<String> texts) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        addService(values, event.application());
        addService(values, labels.get("application"));
        addService(values, labels.get("app"));
        for (String text : texts) {
            addService(values, text);
        }
        return List.copyOf(values);
    }

    private List<String> collectResources(Map<String, String> labels, Set<String> texts) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        addValue(values, labels.get("resource"));
        for (String text : texts) {
            if (text != null && text.contains("/api/v1/")) {
                addValue(values, extractUri(text));
            }
        }
        return List.copyOf(values);
    }

    private List<String> collectFromTexts(String direct, Set<String> knownValues, Set<String> texts) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        addValue(values, direct);
        for (String known : knownValues) {
            for (String text : texts) {
                if (containsToken(text, known)) {
                    values.add(known);
                }
            }
        }
        return List.copyOf(values);
    }

    private void addService(Set<String> values, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String canonical = catalog.canonicalService(raw);
        if (!canonical.isBlank()) {
            values.add(canonical);
        }
    }

    private static void addValue(Set<String> values, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        values.add(OpsKnowledgeCatalog.norm(raw));
    }

    private static void addText(Set<String> texts, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        texts.add(raw.toLowerCase());
    }

    private static boolean containsToken(String text, String token) {
        return text != null && token != null && !token.isBlank() && text.contains(token);
    }

    private static String extractUri(String text) {
        int start = text.indexOf("/api/v1/");
        if (start < 0) {
            return text;
        }
        int end = start;
        while (end < text.length() && !Character.isWhitespace(text.charAt(end))
                && text.charAt(end) != '"' && text.charAt(end) != '\'' && text.charAt(end) != ',' && text.charAt(end) != ';') {
            end++;
        }
        return text.substring(start, end);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
