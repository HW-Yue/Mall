package com.yue.opsagent.springai.domain.alert;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class AlertEnrichmentService {

    private final AlertSignalResolver signalResolver;
    private final OpsKnowledgeCatalog catalog;

    public AlertEnrichmentService(AlertSignalResolver signalResolver, OpsKnowledgeCatalog catalog) {
        this.signalResolver = signalResolver;
        this.catalog = catalog;
    }

    public EnrichedAlertContext enrich(AlertEvent event) {
        AlertSignals signals = signalResolver.resolve(event);
        Map<String, Integer> scores = new LinkedHashMap<>();
        Map<String, List<String>> reasons = new LinkedHashMap<>();

        for (String application : signals.applications()) {
            score(scores, reasons, application, 100, "application/app 直接命中");
        }
        for (String resource : signals.resources()) {
            catalog.ownerByResource(resource).ifPresent(service ->
                    score(scores, reasons, service, 85, "resource 命中 " + resource));
        }
        for (String group : signals.consumerGroups()) {
            for (String service : catalog.ownersByConsumerGroup(group)) {
                score(scores, reasons, service, 95, "consumerGroup 命中 " + group);
            }
        }
        for (String topic : signals.topics()) {
            for (String service : catalog.consumersByTopic(topic)) {
                score(scores, reasons, service, 70, "topic 消费者命中 " + topic);
            }
            for (String service : catalog.producersByTopic(topic)) {
                score(scores, reasons, service, 50, "topic 生产者命中 " + topic);
            }
        }
        for (String table : signals.tables()) {
            for (String service : catalog.ownersByTable(table)) {
                score(scores, reasons, service, 80, "table 命中 " + table);
            }
        }
        for (String database : signals.databases()) {
            for (String service : catalog.ownersByDatabase(database)) {
                score(scores, reasons, service, 80, "db 命中 " + database);
            }
        }
        for (String pool : signals.pools()) {
            catalog.ownerByPool(pool).ifPresent(service ->
                    score(scores, reasons, service, 90, "pool 命中 " + pool));
        }

        List<String> candidates = scores.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        String primary = candidates.isEmpty() ? "" : candidates.getFirst();
        String resource = first(signals.resources());
        String topic = first(signals.topics());
        String consumerGroup = first(signals.consumerGroups());
        String table = first(signals.tables());
        String database = first(signals.databases());
        String pool = first(signals.pools());

        Map<String, String> resolvedLabels = new LinkedHashMap<>();
        if (event.labels() != null) {
            resolvedLabels.putAll(event.labels());
        }
        if (!primary.isBlank()) {
            resolvedLabels.put("application", primary);
        }
        putIfNotBlank(resolvedLabels, "resource", resource);
        putIfNotBlank(resolvedLabels, "topic", topic);
        putIfNotBlank(resolvedLabels, "consumerGroup", consumerGroup);
        putIfNotBlank(resolvedLabels, "table", table);
        putIfNotBlank(resolvedLabels, "database", database);
        putIfNotBlank(resolvedLabels, "pool", pool);

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("signals", Map.of(
                "applications", signals.applications(),
                "resources", signals.resources(),
                "topics", signals.topics(),
                "consumerGroups", signals.consumerGroups(),
                "tables", signals.tables(),
                "databases", signals.databases(),
                "pools", signals.pools()));
        evidence.put("reasonByService", reasons);

        return new EnrichedAlertContext(
                primary,
                candidates,
                resource,
                topic,
                consumerGroup,
                table,
                database,
                pool,
                primary.isBlank() ? "" : String.join("；", reasons.getOrDefault(primary, List.of())),
                Map.copyOf(resolvedLabels),
                Map.copyOf(evidence));
    }

    private static void score(
            Map<String, Integer> scores,
            Map<String, List<String>> reasons,
            String service,
            int delta,
            String reason) {
        if (service == null || service.isBlank()) {
            return;
        }
        scores.merge(service, delta, Integer::sum);
        reasons.computeIfAbsent(service, ignored -> new ArrayList<>()).add(reason);
    }

    private static String first(List<String> values) {
        return values == null || values.isEmpty() ? "" : values.getFirst();
    }

    private static void putIfNotBlank(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }
}
