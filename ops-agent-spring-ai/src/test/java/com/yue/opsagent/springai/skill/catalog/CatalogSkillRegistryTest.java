package com.yue.opsagent.springai.skill.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.domain.alert.AlertEnrichmentService;
import com.yue.opsagent.springai.domain.alert.AlertSignalResolver;
import com.yue.opsagent.springai.domain.alert.OpsKnowledgeCatalog;
import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogSkillRegistryTest {

    private final OpsKnowledgeCatalog catalog = new OpsKnowledgeCatalog(
            new OpsAiProperties(),
            new DefaultResourceLoader(),
            new ObjectMapper());

    private final CatalogSkillRegistry registry = new CatalogSkillRegistry(
            new CatalogToolkit(catalog, new AlertEnrichmentService(new AlertSignalResolver(catalog), catalog)));

    @Test
    void resolveServiceReturnsPrimaryServiceAndContainer() {
        ToolResult result = registry.execute("catalog_resolve_service", Map.of(
                "query", "nexus-order-service 下单接口超时，需要排查"));

        assertThat(result).isInstanceOf(ToolResult.Ok.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.toMap().get("data");
        assertThat(data).containsEntry("primaryService", "order-service");
        @SuppressWarnings("unchecked")
        Map<String, Object> serviceProfile = (Map<String, Object>) data.get("serviceProfile");
        assertThat(serviceProfile).containsEntry("containerName", "nexus-order-service");
    }

    @Test
    void describeServiceReturnsStaticTopology() {
        ToolResult result = registry.execute("catalog_describe_service", Map.of("service", "gateway"));

        assertThat(result).isInstanceOf(ToolResult.Ok.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.toMap().get("data");
        assertThat(data).containsEntry("service", "gateway");
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) data.get("profile");
        assertThat(profile).containsEntry("application", "springcloud-gateway");
        assertThat(profile).containsEntry("containerName", "nexus-gateway");
        assertThat(data.get("aliases").toString()).contains("springcloud-gateway");
    }

    @Test
    void listServicesAndTopicsReturnStableLightweightCatalogViews() {
        ToolResult services = registry.execute("catalog_list_services", Map.of());
        ToolResult topics = registry.execute("catalog_list_topics", Map.of());

        assertThat(services).isInstanceOf(ToolResult.Ok.class);
        assertThat(topics).isInstanceOf(ToolResult.Ok.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> serviceData = (Map<String, Object>) services.toMap().get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> topicData = (Map<String, Object>) topics.toMap().get("data");
        @SuppressWarnings("unchecked")
        List<String> serviceNames = (List<String>) serviceData.get("services");
        @SuppressWarnings("unchecked")
        List<String> topicNames = (List<String>) topicData.get("topics");

        assertThat(serviceData).containsEntry("count", serviceNames.size());
        assertThat(topicData).containsEntry("count", topicNames.size());
        assertThat(serviceNames).contains("mall-service", "order-service", "group-buy-service", "seckill-service", "pay-service");
        assertThat(serviceNames).isSorted();
        assertThat(topicNames).contains("group-buy-order-create", "pay-success-normal", "seckill-order-create");
        assertThat(topicNames).isSorted();
    }

    @Test
    void lookupOwnerReturnsTopicProducersConsumersAndHelpDocs() {
        ToolResult result = registry.execute("catalog_lookup_resource_owner", Map.of(
                "kind", "topic",
                "value", "group-buy-order-create"));

        assertThat(result).isInstanceOf(ToolResult.Ok.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.toMap().get("data");
        assertThat(data.get("producers").toString()).contains("order-service");
        assertThat(data.get("consumers").toString()).contains("order-service");
        assertThat(data.get("consumerGroups").toString()).contains("cg_group_buy_order_create");

        ToolResult help = registry.execute(
                OpsSkillRegistry.DEFAULT_HELP_TOOL_NAME,
                Map.of(OpsSkillRegistry.HELP_ARG_TOOL, "catalog_list_services"));
        assertThat(help).isInstanceOf(ToolResult.Ok.class);
        assertThat(help.toMap().get("data").toString()).contains("services");
    }
}
