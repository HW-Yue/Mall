package com.yue.opsagent.springai.domain.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class OpsKnowledgeCatalogTest {

    private final OpsKnowledgeCatalog catalog = new OpsKnowledgeCatalog(
            new OpsAiProperties(),
            new DefaultResourceLoader(),
            new ObjectMapper());

    @Test
    void loadsServiceProfileAndReverseLookups() {
        assertThat(catalog.serviceProfile("gateway"))
                .isPresent()
                .get()
                .extracting(
                        OpsKnowledgeCatalog.ServiceProfile::application,
                        OpsKnowledgeCatalog.ServiceProfile::composeService,
                        OpsKnowledgeCatalog.ServiceProfile::containerName)
                .containsExactly("springcloud-gateway", "gateway", "nexus-gateway");

        assertThat(catalog.resourcesByService("order-service")).contains("/api/v1/order/get_pay_url");
        assertThat(catalog.poolsByService("order-service")).contains("order_hikaricp");
        assertThat(catalog.aliasesForService("order-service")).contains("order", "nexus-order-service", "order-service");
    }
}
