package com.yue.opsagent.springai.domain.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AlertEnrichmentServiceTest {

    private final OpsKnowledgeCatalog catalog = new OpsKnowledgeCatalog(
            new OpsAiProperties(),
            new DefaultResourceLoader(),
            new ObjectMapper());

    private final AlertEnrichmentService service =
            new AlertEnrichmentService(new AlertSignalResolver(catalog), catalog);

    @Test
    void resolvesServiceFromResource() {
        AlertEvent event = new AlertEvent(
                "firing",
                "SentinelRtHigh",
                "warning",
                "",
                Map.of(
                        "category", "sentinel",
                        "resource", "/api/v1/order/get_pay_url"),
                Map.of());

        EnrichedAlertContext context = service.enrich(event);

        assertThat(context.primaryService()).isEqualTo("order-service");
        assertThat(context.resource()).isEqualTo("/api/v1/order/get_pay_url");
    }

    @Test
    void resolvesServiceFromTopicAndConsumerGroup() {
        AlertEvent event = new AlertEvent(
                "firing",
                "RocketMqConsumerLagHigh",
                "warning",
                "shared",
                Map.of(
                        "category", "rocketmq",
                        "topic", "group-buy-order-create",
                        "consumerGroup", "CG_GROUP_BUY_ORDER_CREATE"),
                Map.of());

        EnrichedAlertContext context = service.enrich(event);

        assertThat(context.primaryService()).isEqualTo("order-service");
        assertThat(context.candidateServices()).contains("order-service");
        assertThat(context.topic()).isEqualTo("group-buy-order-create");
        assertThat(context.consumerGroup()).isEqualTo("cg_group_buy_order_create");
    }

    @Test
    void resolvesServiceFromTableAndPool() {
        AlertEvent event = new AlertEvent(
                "firing",
                "HikariConnectionsSaturated",
                "critical",
                "",
                Map.of(
                        "category", "hikari",
                        "pool", "Order_HikariCP"),
                Map.of(
                        "description", "最近 SQL 涉及 t_order 表，连接池等待变多"));

        EnrichedAlertContext context = service.enrich(event);

        assertThat(context.primaryService()).isEqualTo("order-service");
        assertThat(context.table()).isEqualTo("t_order");
        assertThat(context.pool()).isEqualTo("order_hikaricp");
    }

    @Test
    void resolvesServiceFromContainerAliasInText() {
        AlertEvent event = new AlertEvent(
                "firing",
                "PlainTextOpsRequest",
                "warning",
                "",
                Map.of("category", "text"),
                Map.of("summary", "nexus-order-service 最近下单接口超时"));

        EnrichedAlertContext context = service.enrich(event);

        assertThat(context.primaryService()).isEqualTo("order-service");
        assertThat(context.candidateServices()).contains("order-service");
    }
}
