package com.yue.opsagent.springai.skill.catalog;

import com.yue.opsagent.springai.domain.alert.AlertEnrichmentService;
import com.yue.opsagent.springai.domain.alert.AlertEvent;
import com.yue.opsagent.springai.domain.alert.EnrichedAlertContext;
import com.yue.opsagent.springai.domain.alert.OpsKnowledgeCatalog;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CatalogToolkit {

    private final OpsKnowledgeCatalog catalog;
    private final AlertEnrichmentService alertEnrichmentService;

    public CatalogToolkit(
            OpsKnowledgeCatalog catalog,
            AlertEnrichmentService alertEnrichmentService) {
        this.catalog = catalog;
        this.alertEnrichmentService = alertEnrichmentService;
    }

    public ToolResult resolveService(
            String query,
            String service,
            String application,
            String resource,
            String topic,
            String consumerGroup,
            String table,
            String database,
            String pool) {
        if (allBlank(query, service, application, resource, topic, consumerGroup, table, database, pool)) {
            return ToolResult.error("catalog_resolve_service: 至少提供 query 或一个结构化线索");
        }
        AlertEvent event = buildEvent(query, service, application, resource, topic, consumerGroup, table, database, pool);
        EnrichedAlertContext enrichment = alertEnrichmentService.enrich(event);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("primaryService", enrichment.primaryService());
        data.put("candidateServices", enrichment.candidateServices());
        data.put("serviceReason", enrichment.serviceReason());
        data.put("resolvedLabels", enrichment.resolvedLabels());
        data.put("evidence", enrichment.evidence());
        data.put("serviceProfile", serviceProfileData(enrichment.primaryService()));
        String message = enrichment.primaryService().isBlank()
                ? "未解析到明确服务"
                : "已解析主服务 " + enrichment.primaryService();
        return ToolResult.ok(message, data);
    }

    public ToolResult listServices() {
        List<String> services = catalog.knownCanonicalServices();
        return ToolResult.ok("已返回服务名清单", Map.of(
                "services", services,
                "count", services.size()));
    }

    public ToolResult listTopics() {
        List<String> topics = catalog.topicNames();
        return ToolResult.ok("已返回 Topic 清单", Map.of(
                "topics", topics,
                "count", topics.size()));
    }

    public ToolResult describeService(String service) {
        String canonical = canonicalService(service);
        if (canonical.isBlank()) {
            return ToolResult.ok("未知服务", Map.of("found", false, "input", nullToEmpty(service)));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("found", true);
        data.put("service", canonical);
        data.put("profile", serviceProfileData(canonical));
        data.put("aliases", catalog.aliasesForService(canonical));
        data.put("resources", catalog.resourcesByService(canonical));
        data.put("topicsProduced", catalog.producedTopicsByService(canonical));
        data.put("topicsConsumed", catalog.consumedTopicsByService(canonical));
        data.put("consumerGroups", catalog.consumerGroupsByService(canonical));
        data.put("tables", catalog.tablesByService(canonical));
        data.put("databases", catalog.databasesByService(canonical));
        data.put("pools", catalog.poolsByService(canonical));
        return ToolResult.ok("已返回服务静态拓扑 " + canonical, data);
    }

    public ToolResult lookupOwner(String kind, String value) {
        String normalizedKind = OpsKnowledgeCatalog.norm(kind);
        String normalizedValue = OpsKnowledgeCatalog.norm(value);
        if (normalizedKind.isBlank() || normalizedValue.isBlank()) {
            return ToolResult.error("catalog_lookup_resource_owner: kind 和 value 均不能为空");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("kind", normalizedKind);
        data.put("value", normalizedValue);
        switch (normalizedKind) {
            case "resource" -> {
                String owner = catalog.ownerByResource(normalizedValue).orElse("");
                data.put("ownerService", owner);
                data.put("serviceProfile", serviceProfileData(owner));
                return ToolResult.ok(owner.isBlank() ? "未找到 resource owner" : "已找到 resource owner", data);
            }
            case "topic" -> {
                List<String> producers = catalog.producersByTopic(normalizedValue);
                List<String> consumers = catalog.consumersByTopic(normalizedValue);
                data.put("producers", producers);
                data.put("consumers", consumers);
                data.put("consumerGroups", topicConsumerGroups(normalizedValue));
                data.put("serviceProfiles", serviceProfilesData(mergeDistinct(producers, consumers)));
                return ToolResult.ok(producers.isEmpty() && consumers.isEmpty()
                        ? "未找到 topic 归属"
                        : "已返回 topic 静态归属", data);
            }
            case "consumergroup", "group" -> {
                List<String> owners = catalog.ownersByConsumerGroup(normalizedValue);
                data.put("ownerServices", owners);
                data.put("serviceProfiles", serviceProfilesData(owners));
                return ToolResult.ok(owners.isEmpty() ? "未找到 consumerGroup owner" : "已找到 consumerGroup owner", data);
            }
            case "table" -> {
                List<String> owners = catalog.ownersByTable(normalizedValue);
                data.put("ownerServices", owners);
                data.put("serviceProfiles", serviceProfilesData(owners));
                return ToolResult.ok(owners.isEmpty() ? "未找到 table owner" : "已找到 table owner", data);
            }
            case "database", "db" -> {
                List<String> owners = catalog.ownersByDatabase(normalizedValue);
                data.put("ownerServices", owners);
                data.put("serviceProfiles", serviceProfilesData(owners));
                return ToolResult.ok(owners.isEmpty() ? "未找到 database owner" : "已找到 database owner", data);
            }
            case "pool" -> {
                String owner = catalog.ownerByPool(normalizedValue).orElse("");
                data.put("ownerService", owner);
                data.put("serviceProfile", serviceProfileData(owner));
                return ToolResult.ok(owner.isBlank() ? "未找到 pool owner" : "已找到 pool owner", data);
            }
            default -> {
                return ToolResult.error("catalog_lookup_resource_owner: kind 仅支持 resource/topic/consumerGroup/table/database/pool");
            }
        }
    }

    private AlertEvent buildEvent(
            String query,
            String service,
            String application,
            String resource,
            String topic,
            String consumerGroup,
            String table,
            String database,
            String pool) {
        Map<String, String> labels = new LinkedHashMap<>();
        putLabelIfNotBlank(labels, "resource", resource);
        putLabelIfNotBlank(labels, "topic", topic);
        putLabelIfNotBlank(labels, "consumerGroup", consumerGroup);
        putLabelIfNotBlank(labels, "table", table);
        putLabelIfNotBlank(labels, "database", database);
        putLabelIfNotBlank(labels, "pool", pool);
        Map<String, String> annotations = query == null || query.isBlank()
                ? Map.of()
                : Map.of("summary", query);
        String eventApplication = firstNonBlank(application, service);
        return new AlertEvent(
                "firing",
                "CatalogResolveService",
                "info",
                eventApplication,
                labels,
                annotations);
    }

    private Map<String, Object> serviceProfileData(String serviceOrAlias) {
        String canonical = canonicalService(serviceOrAlias);
        if (canonical.isBlank()) {
            return Map.of();
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", canonical);
        catalog.serviceProfile(canonical).ifPresent(profile -> {
            putObjectIfNotBlank(data, "application", profile.application());
            putObjectIfNotBlank(data, "composeService", profile.composeService());
            putObjectIfNotBlank(data, "containerName", profile.containerName());
        });
        return data;
    }

    private Map<String, Object> serviceProfilesData(List<String> services) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String service : services) {
            out.put(service, serviceProfileData(service));
        }
        return out;
    }

    private List<String> topicConsumerGroups(String topic) {
        return catalog.snapshot().topics().getOrDefault(
                OpsKnowledgeCatalog.norm(topic),
                new OpsKnowledgeCatalog.TopicProfile(List.of(), List.of(), List.of()))
                .consumerGroups();
    }

    private String canonicalService(String serviceOrAlias) {
        return catalog.canonicalService(serviceOrAlias);
    }

    private static List<String> mergeDistinct(List<String> left, List<String> right) {
        LinkedHashMap<String, Boolean> merged = new LinkedHashMap<>();
        left.forEach(v -> merged.put(v, Boolean.TRUE));
        right.forEach(v -> merged.put(v, Boolean.TRUE));
        return List.copyOf(merged.keySet());
    }

    private static boolean allBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static void putLabelIfNotBlank(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private static void putObjectIfNotBlank(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
