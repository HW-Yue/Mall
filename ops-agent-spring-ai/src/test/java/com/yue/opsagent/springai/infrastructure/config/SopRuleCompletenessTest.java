package com.yue.opsagent.springai.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SopRuleCompletenessTest {

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory())
            .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void allBundledSopRulesAreDeterministicMultiStepPlaybooks() throws Exception {
        List<Resource> resources = loadRules();

        assertThat(resources).isNotEmpty();
        for (Resource resource : resources) {
            OpsAiProperties.Sop.Rule rule = yaml.readValue(
                    resource.getInputStream(),
                    OpsAiProperties.Sop.Rule.class);

            assertThat(rule.getSteps())
                    .as(resource.getFilename() + " should not be a placeholder SOP")
                    .hasSizeGreaterThanOrEqualTo(4);
            assertThat(rule.getSteps())
                    .as(resource.getFilename() + " should delegate at least one domain sub-agent")
                    .anySatisfy(step -> assertThat(step.getType()).isEqualTo("delegate_subagent"));
            assertThat(rule.getSteps())
                    .as(resource.getFilename() + " should contain actionable tasks")
                    .allSatisfy(step -> {
                        if ("delegate_subagent".equals(step.getType())) {
                            assertThat(step.getSubAgentId()).isNotBlank();
                            assertThat(step.getTask()).isNotBlank();
                        }
                    });
        }
    }

    private static List<Resource> loadRules() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<Resource> resources = new ArrayList<>();
        resources.addAll(Arrays.asList(resolver.getResources("classpath:sop/rules/*.yml")));
        resources.addAll(Arrays.asList(resolver.getResources("classpath:sop/rules/*.yaml")));
        resources.sort(Comparator.comparing(resource -> resource.getFilename() == null ? "" : resource.getFilename()));
        return resources;
    }
}
