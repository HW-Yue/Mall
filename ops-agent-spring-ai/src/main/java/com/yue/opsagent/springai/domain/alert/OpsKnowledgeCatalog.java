package com.yue.opsagent.springai.domain.alert;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OpsKnowledgeCatalog {

    private final Snapshot snapshot;

    public OpsKnowledgeCatalog(
            OpsAiProperties properties,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper) {
        this.snapshot = loadSnapshot(properties.getCatalog().getLocation(), resourceLoader, objectMapper);
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    public String canonicalService(String value) {
        return snapshot.serviceAliases().getOrDefault(norm(value), "");
    }

    public Optional<String> ownerByResource(String resource) {
        if (resource == null || resource.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(snapshot.resourceOwners().get(norm(resource)));
    }

    public List<String> consumersByTopic(String topic) {
        TopicProfile profile = snapshot.topics().get(norm(topic));
        return profile == null ? List.of() : profile.consumers();
    }

    public List<String> producersByTopic(String topic) {
        TopicProfile profile = snapshot.topics().get(norm(topic));
        return profile == null ? List.of() : profile.producers();
    }

    public List<String> ownersByConsumerGroup(String group) {
        return snapshot.consumerGroups().getOrDefault(norm(group), List.of());
    }

    public List<String> ownersByTable(String table) {
        return snapshot.tableOwners().getOrDefault(norm(table), List.of());
    }

    public List<String> ownersByDatabase(String database) {
        return snapshot.databaseOwners().getOrDefault(norm(database), List.of());
    }

    public Optional<String> ownerByPool(String pool) {
        return Optional.ofNullable(snapshot.poolOwners().get(norm(pool)));
    }

    public Set<String> knownTopics() {
        return snapshot.topics().keySet();
    }

    public Set<String> knownConsumerGroups() {
        return snapshot.consumerGroups().keySet();
    }

    public Set<String> knownTables() {
        return snapshot.tableOwners().keySet();
    }

    public Set<String> knownDatabases() {
        return snapshot.databaseOwners().keySet();
    }

    public Set<String> knownPools() {
        return snapshot.poolOwners().keySet();
    }

    private static Snapshot loadSnapshot(String location, ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        try {
            Resource resource = resourceLoader.getResource(location);
            try (InputStream in = resource.getInputStream()) {
                Snapshot raw = objectMapper.readValue(in, Snapshot.class);
                return raw.normalized();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ops catalog from " + location, e);
        }
    }

    static String norm(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Snapshot(
            Map<String, String> serviceAliases,
            Map<String, String> resourceOwners,
            Map<String, TopicProfile> topics,
            Map<String, List<String>> consumerGroups,
            Map<String, List<String>> tableOwners,
            Map<String, List<String>> databaseOwners,
            Map<String, String> poolOwners
    ) {
        Snapshot normalized() {
            return new Snapshot(
                    normalizeServiceAliasMap(serviceAliases),
                    normalizeServiceMap(resourceOwners),
                    normalizeTopics(topics),
                    normalizeServiceListMap(consumerGroups),
                    normalizeServiceListMap(tableOwners),
                    normalizeServiceListMap(databaseOwners),
                    normalizeServiceMap(poolOwners));
        }

        private static Map<String, String> normalizeServiceAliasMap(Map<String, String> values) {
            Map<String, String> normalized = new LinkedHashMap<>(normalizeServiceMap(values));
            List.copyOf(normalized.values()).forEach(service -> normalized.putIfAbsent(service, service));
            return Map.copyOf(normalized);
        }

        private static Map<String, String> normalizeServiceMap(Map<String, String> values) {
            if (values == null) {
                return Map.of();
            }
            return values.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                    e -> norm(e.getKey()),
                    e -> norm(e.getValue()),
                    (a, b) -> a));
        }

        private static Map<String, TopicProfile> normalizeTopics(Map<String, TopicProfile> values) {
            if (values == null) {
                return Map.of();
            }
            return values.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                    e -> norm(e.getKey()),
                    e -> e.getValue() == null ? new TopicProfile(List.of(), List.of(), List.of()) : e.getValue().normalized(),
                    (a, b) -> a));
        }

        private static Map<String, List<String>> normalizeServiceListMap(Map<String, List<String>> values) {
            if (values == null) {
                return Map.of();
            }
            return values.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                    e -> norm(e.getKey()),
                    e -> e.getValue() == null ? List.of() : e.getValue().stream()
                            .map(OpsKnowledgeCatalog::norm)
                            .filter(s -> !s.isBlank())
                            .distinct()
                            .toList(),
                    (a, b) -> a));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TopicProfile(
            List<String> producers,
            List<String> consumers,
            List<String> consumerGroups
    ) {
        TopicProfile normalized() {
            return new TopicProfile(normalize(producers), normalize(consumers), normalize(consumerGroups));
        }

        private static List<String> normalize(List<String> values) {
            return values == null ? List.of() : values.stream()
                    .map(OpsKnowledgeCatalog::norm)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();
        }
    }
}
