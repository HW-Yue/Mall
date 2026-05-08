package com.yue.opsagent.springai.domain.alert;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AlertPlaceholderResolverTest {

    @Test
    void flattensEnrichmentFieldsForSopTemplates() {
        AlertEvent event = new AlertEvent(
                "firing",
                "RocketMqConsumerLagHigh",
                "warning",
                "shared",
                Map.of("category", "rocketmq"),
                Map.of());
        EnrichedAlertContext enrichment = new EnrichedAlertContext(
                "order-service",
                List.of("order-service", "group-buy-service"),
                "",
                "group-buy-order-create",
                "CG_GROUP_BUY_ORDER_CREATE",
                "t_order",
                "order_service",
                "Order_HikariCP",
                "topic 与 group 命中",
                Map.of(),
                Map.of());

        Map<String, String> flat = AlertPlaceholderResolver.flatten(event, enrichment);

        assertThat(flat)
                .containsEntry("primaryService", "order-service")
                .containsEntry("topic", "group-buy-order-create")
                .containsEntry("consumerGroup", "CG_GROUP_BUY_ORDER_CREATE")
                .containsEntry("table", "t_order")
                .containsEntry("database", "order_service");
        assertThat(AlertPlaceholderResolver.substituteTemplate(
                "服务=${primaryService}, topic=${topic}, 表=${table}", flat))
                .isEqualTo("服务=order-service, topic=group-buy-order-create, 表=t_order");
    }
}
