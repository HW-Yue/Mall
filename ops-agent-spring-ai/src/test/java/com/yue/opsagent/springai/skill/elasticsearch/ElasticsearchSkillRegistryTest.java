package com.yue.opsagent.springai.skill.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ElasticsearchSkillRegistryTest {

    @Test
    void dispatchesServiceErrorSearchToHighLevelToolkitMethod() {
        ElasticsearchToolkit toolkit = mock(ElasticsearchToolkit.class);
        when(toolkit.searchServiceErrors(
                eq("logs"),
                eq(""),
                eq("order-service"),
                eq("order-service"),
                eq("2h"),
                eq(5),
                eq(List.of("timeout", "deadlock"))))
                .thenReturn(ToolResult.ok("服务错误日志摘要", Map.of("totalHits", 2)));
        ElasticsearchSkillRegistry registry = new ElasticsearchSkillRegistry(toolkit);

        ToolResult result = registry.execute("es_search_service_errors", Map.of(
                "service", "order-service",
                "application", "order-service",
                "lookback", "2h",
                "size", 5,
                "keywords", List.of("timeout", "deadlock")));

        assertThat(result).isInstanceOf(ToolResult.Ok.class);
        verify(toolkit).searchServiceErrors("logs", "", "order-service", "order-service", "2h", 5, List.of("timeout", "deadlock"));
    }

    @Test
    void normalizeSearchBodyWrapsBareQueryClause() {
        ElasticsearchToolkit toolkit = new ElasticsearchToolkit(new OpsAiProperties(), new ObjectMapper());

        String body = toolkit.normalizeSearchBody("""
                {"bool":{"must":[{"term":{"service.keyword":"order-service"}}]}}
                """);

        assertThat(body).contains("\"query\"");
        assertThat(body).contains("\"service.keyword\":\"order-service\"");
    }

    @Test
    void normalizeSearchBodyRejectsInvalidJson() {
        ElasticsearchToolkit toolkit = new ElasticsearchToolkit(new OpsAiProperties(), new ObjectMapper());

        assertThatThrownBy(() -> toolkit.normalizeSearchBody("{"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON 非法");
    }
}
