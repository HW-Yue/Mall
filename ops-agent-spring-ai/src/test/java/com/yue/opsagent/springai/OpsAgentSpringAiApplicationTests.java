package com.yue.opsagent.springai;

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
    }

    @Test
    void sevenSubAgentsRegistered() {
        assertThat(ISubAgents).hasSize(7);
        assertThat(ISubAgents.stream().map(ISubAgent::domainId).distinct()).hasSize(7);
    }

    @Test
    void externalSopRuleFilesLoaded() {
        var rules = opsAiProperties.getSop().getRules();
        assertThat(rules).hasSize(24);
        assertThat(rules)
                .extracting(r -> r.getMatchAlertname().toLowerCase())
                .contains(
                        "servicedown",
                        "http5xxerrorratehigh",
                        "rocketmqconsumerlaghigh",
                        "nacosconfigdrill");
    }
}
