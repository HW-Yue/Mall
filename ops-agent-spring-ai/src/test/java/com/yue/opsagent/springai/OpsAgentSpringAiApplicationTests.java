package com.yue.opsagent.springai;

import com.yue.opsagent.springai.agent.registry.AgentToolRegistry;
import com.yue.opsagent.springai.agent.sub.ISubAgent;
import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.skill.registry.MasterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.ai.dashscope.api-key=dummy-key-for-test",
        "ops-ai.otlp.enabled=false"
})
class OpsAgentSpringAiApplicationTests {

    @Autowired
    private MasterRegistry masterRegistry;

    @Autowired
    private AgentToolRegistry agentToolRegistry;

    @Autowired
    private OpsAiProperties opsAiProperties;

    @Autowired
    private List<ISubAgent> ISubAgents;

    @Test
    void contextLoads() {
        String menu = masterRegistry.buildMenu();
        assertThat(menu).isNotBlank();
        assertThat(menu).contains("metrics_ops");
        assertThat(menu).contains("docker_ops");
        assertThat(menu).contains("mysql_inspect");
        assertThat(menu).contains("rocketmq_inspect");
        assertThat(menu).contains("elasticsearch_ops");
        assertThat(menu).contains("redis_inspect");
        assertThat(menu).contains("nacos_config");
        assertThat(menu).contains("catalog_ops");
    }

    @Test
    void parentAgentMenuUsesSkillToolNames() {
        String menu = agentToolRegistry.buildMenu();
        assertThat(menu).contains("Docker Skill (tool: docker_skill)");
        assertThat(menu).contains("Prometheus Skill (tool: prometheus_skill)");
        assertThat(menu).contains("Nacos Skill (tool: nacos_skill)");
        assertThat(menu).contains("Catalog Skill (tool: catalog_skill)");
        assertThat(menu).doesNotContain("tool: nacos_config");
        assertThat(menu).doesNotContain("tool: mysql_inspect");
    }

    @Test
    void eightSubAgentsRegistered() {
        assertThat(ISubAgents).hasSize(8);
        assertThat(ISubAgents.stream().map(ISubAgent::domainId).distinct()).hasSize(8);
    }

    @Test
    void externalSopRuleFilesLoaded() {
        var rules = opsAiProperties.getSop().getRules();
        assertThat(rules).hasSize(33);
        assertThat(rules)
                .extracting(r -> r.getMatchAlertname().toLowerCase())
                .contains(
                        "servicedown",
                        "http5xxerrorratehigh",
                        "rocketmqconsumerlaghigh",
                        "nacosconfigdrill");
    }
}
