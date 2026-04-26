package com.yue.opsagent.springai.agent.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.agent.registry.rule.AgentCancelRuleFilter;
import com.yue.opsagent.springai.agent.registry.rule.AgentExecuteRuleFilter;
import com.yue.opsagent.springai.agent.registry.rule.AgentResolveRuleFilter;
import com.yue.opsagent.springai.agent.registry.rule.AgentResultRecordRuleFilter;
import com.yue.opsagent.springai.agent.registry.rule.AgentTaskValidateRuleFilter;
import com.yue.opsagent.springai.agent.registry.rule.AgentTraceStartRuleFilter;
import com.yue.opsagent.springai.agent.sub.ISubAgent;
import com.yue.opsagent.springai.domain.opsroute.OpsRunContextHolder;
import com.yue.opsagent.springai.domain.opsroute.OpsRunEvent;
import com.yue.opsagent.springai.domain.opsroute.RouteRequest;
import com.yue.opsagent.springai.service.OpsRunService;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AgentToolRegistryRuleChainTest {

    @Test
    void exposesClearSkillToolNameAndExecutesSubAgent() {
        TestSubAgent agent = new TestSubAgent();
        OpsRunService opsRunService = new OpsRunService(new ObjectMapper());
        AgentToolRegistry registry = agentToolRegistry(opsRunService, agent);
        String runId = opsRunService.create(RouteRequest.text("test")).runId();

        String result = executeWithRun(runId, () ->
                registry.execute("nacos_skill", "查询服务实例", Map.of("service", "mall")));

        assertThat(result).isEqualTo("summary:查询服务实例");
        assertThat(agent.executions()).isEqualTo(1);
        assertSubAgentEvent(opsRunService, runId, "execute", "success", "nacos_skill");
    }

    @Test
    void keepsDomainIdAliasForSopCompatibility() {
        TestSubAgent agent = new TestSubAgent();
        OpsRunService opsRunService = new OpsRunService(new ObjectMapper());
        AgentToolRegistry registry = agentToolRegistry(opsRunService, agent);
        String runId = opsRunService.create(RouteRequest.text("test")).runId();

        String result = executeWithRun(runId, () ->
                registry.execute("nacos_config", "查询配置", Map.of()));

        assertThat(result).isEqualTo("summary:查询配置");
        assertThat(agent.executions()).isEqualTo(1);
        assertSubAgentEvent(opsRunService, runId, "execute", "success", "nacos_config");
    }

    @Test
    void rejectsBlankTaskBeforeSubAgentExecution() {
        TestSubAgent agent = new TestSubAgent();
        OpsRunService opsRunService = new OpsRunService(new ObjectMapper());
        AgentToolRegistry registry = agentToolRegistry(opsRunService, agent);
        String runId = opsRunService.create(RouteRequest.text("test")).runId();

        String result = executeWithRun(runId, () ->
                registry.execute("nacos_skill", " ", Map.of()));

        assertThat(result).isEqualTo("子Agent任务不能为空: nacos_skill");
        assertThat(agent.executions()).isZero();
        assertSubAgentEvent(opsRunService, runId, "validate", "error", "nacos_skill");
    }

    @Test
    void rejectsUnknownSubAgentAndRecordsEvent() {
        TestSubAgent agent = new TestSubAgent();
        OpsRunService opsRunService = new OpsRunService(new ObjectMapper());
        AgentToolRegistry registry = agentToolRegistry(opsRunService, agent);
        String runId = opsRunService.create(RouteRequest.text("test")).runId();

        String result = executeWithRun(runId, () ->
                registry.execute("unknown_skill", "查询", Map.of()));

        assertThat(result).isEqualTo("unknown sub agent tool: unknown_skill");
        assertThat(agent.executions()).isZero();
        assertSubAgentEvent(opsRunService, runId, "resolve", "error", "unknown_skill");
    }

    @Test
    void exposesReactToolsThatDelegateToSubAgents() {
        TestSubAgent agent = new TestSubAgent();
        OpsRunService opsRunService = new OpsRunService(new ObjectMapper());
        AgentToolRegistry registry = agentToolRegistry(opsRunService, agent);
        String runId = opsRunService.create(RouteRequest.text("test")).runId();

        String result = executeWithRun(runId, () -> registry.reactTools(() -> Map.of("service", "mall")).stream()
                .filter(t -> "nacos_skill".equals(t.name()))
                .findFirst()
                .orElseThrow()
                .execute(Map.of("task", "查询服务实例"), Map.of()));

        assertThat(result).isEqualTo("summary:查询服务实例");
        assertThat(agent.executions()).isEqualTo(1);
        assertSubAgentEvent(opsRunService, runId, "execute", "success", "nacos_skill");
    }

    private static AgentToolRegistry agentToolRegistry(OpsRunService opsRunService, ISubAgent agent) {
        AgentToolRuleFilterFactory factory = new AgentToolRuleFilterFactory();
        var chain = factory.agentToolRuleFilter(java.util.List.of(
                new AgentExecuteRuleFilter(),
                new AgentTaskValidateRuleFilter(),
                new AgentTraceStartRuleFilter(),
                new AgentResolveRuleFilter(),
                new AgentCancelRuleFilter(opsRunService),
                new AgentResultRecordRuleFilter(opsRunService, new ObjectMapper())));
        return new AgentToolRegistry(java.util.List.of(agent), chain);
    }

    private static String executeWithRun(String runId, java.util.function.Supplier<String> supplier) {
        OpsRunContextHolder.set(runId);
        try {
            return supplier.get();
        } finally {
            OpsRunContextHolder.clear();
        }
    }

    private static void assertSubAgentEvent(
            OpsRunService opsRunService,
            String runId,
            String phase,
            String outcome,
            String agentTool) {
        OpsRunEvent event = opsRunService.snapshot(runId).orElseThrow().events().stream()
                .filter(e -> "sub_agent_result".equals(e.type()))
                .reduce((first, second) -> second)
                .orElseThrow();
        assertThat(event.data())
                .containsEntry("eventType", "sub_agent_result")
                .containsEntry("phase", phase)
                .containsEntry("outcome", outcome)
                .containsEntry("agentTool", agentTool);
        assertThat(event.data()).containsKeys("agent", "task", "result", "durationMs");
    }

    private static class TestSubAgent implements ISubAgent {

        private final AtomicInteger executions = new AtomicInteger();

        @Override
        public String domainId() {
            return "nacos_config";
        }

        @Override
        public String parentToolDescription() {
            return "Nacos Skill test";
        }

        @Override
        public String runReact(String task, Map<String, Object> context) {
            executions.incrementAndGet();
            return "summary:" + task;
        }

        int executions() {
            return executions.get();
        }
    }
}
