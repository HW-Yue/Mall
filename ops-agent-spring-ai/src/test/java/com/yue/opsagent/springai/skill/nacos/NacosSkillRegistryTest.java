package com.yue.opsagent.springai.skill.nacos;

import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NacosSkillRegistryTest {

    private final NacosSkillRegistry registry = new NacosSkillRegistry(new NacosToolkit(new OpsAiProperties()));

    @Test
    void rejectsWildcardDataIdBeforeCallingNacos() {
        ToolResult result = registry.execute("nacos_get_config", Map.of(
                "dataId", "*",
                "group", "DEFAULT_GROUP"));

        assertThat(result).isInstanceOf(ToolResult.Error.class);
        assertThat(result.toMap().get("message").toString()).contains("不支持通配符");
    }

    @Test
    void rejectsMultipleDataIdsBeforeCallingNacos() {
        ToolResult result = registry.execute("nacos_get_config", Map.of(
                "dataId", "order-service-runtime-dev.yml,pay-service-runtime-dev.yml",
                "group", "DEFAULT_GROUP"));

        assertThat(result).isInstanceOf(ToolResult.Error.class);
        assertThat(result.toMap().get("message").toString()).contains("只允许传单个明确值");
    }
}
