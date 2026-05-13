package com.yue.opsagent.springai.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class JvmGcPauseHighSopRuleTest {

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory())
            .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void jvmGcPauseHighRuleIncludesDeterministicSubAgentSteps() throws Exception {
        OpsAiProperties.Sop.Rule rule = yaml.readValue(
                new ClassPathResource("sop/rules/jvm-gc-pause-high.yml").getInputStream(),
                OpsAiProperties.Sop.Rule.class);

        assertThat(rule.getMatchAlertname()).isEqualTo("JvmGcPauseHigh");
        assertThat(rule.getSteps()).hasSize(5);
        assertThat(rule.getSteps())
                .extracting(OpsAiProperties.Sop.Step::getSubAgentId)
                .containsExactly("metrics_ops", "docker_ops", "docker_ops", "elasticsearch_ops", "nacos_config");
        assertThat(rule.getSteps().get(2).getTask()).contains("jcmd", "jstack");
    }
}
