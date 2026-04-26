package com.yue.opsagent.springai.skill.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.domain.opsroute.OpsRunContextHolder;
import com.yue.opsagent.springai.domain.opsroute.RouteRequest;
import com.yue.opsagent.springai.service.ApprovalService;
import com.yue.opsagent.springai.service.OpsRunService;
import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.registry.rule.RunCancelRuleFilter;
import com.yue.opsagent.springai.skill.registry.rule.SkillResolveRuleFilter;
import com.yue.opsagent.springai.skill.registry.rule.ToolApprovalRuleFilter;
import com.yue.opsagent.springai.skill.registry.rule.ToolExecuteRuleFilter;
import com.yue.opsagent.springai.skill.registry.rule.ToolTraceStartRuleFilter;
import com.yue.opsagent.springai.skill.registry.rule.ToolWhitelistRuleFilter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class MasterRegistryRuleChainTest {

    @Test
    void executesToolThroughWrenchRuleChain() {
        TestSkillRegistry skill = new TestSkillRegistry(false);
        MasterRegistry masterRegistry = masterRegistry(skill);

        ToolResult result = masterRegistry.execute("test_skill", "test_tool", Map.of("k", "v"));

        assertThat(result).isInstanceOf(ToolResult.Ok.class);
        assertThat(skill.executions()).isEqualTo(1);
    }

    @Test
    void rejectsUnknownSkillBeforeToolExecution() {
        TestSkillRegistry skill = new TestSkillRegistry(false);
        MasterRegistry masterRegistry = masterRegistry(skill);

        ToolResult result = masterRegistry.execute("missing_skill", "test_tool", Map.of());

        assertThat(result).isInstanceOf(ToolResult.Error.class);
        assertThat(result.toMap()).containsEntry("message", "unknown skill: missing_skill");
        assertThat(skill.executions()).isZero();
    }

    @Test
    void rejectsUnknownToolBeforeToolExecution() {
        TestSkillRegistry skill = new TestSkillRegistry(false);
        MasterRegistry masterRegistry = masterRegistry(skill);

        ToolResult result = masterRegistry.execute("test_skill", "missing_tool", Map.of());

        assertThat(result).isInstanceOf(ToolResult.Error.class);
        assertThat(result.toMap()).containsEntry("message", "unknown tool for skill test_skill: missing_tool");
        assertThat(skill.executions()).isZero();
    }

    @Test
    void returnsPendingApprovalWithoutExecutingWriteTool() {
        TestSkillRegistry skill = new TestSkillRegistry(true);
        MasterRegistry masterRegistry = masterRegistry(skill);

        ToolResult result = masterRegistry.execute("test_skill", "test_tool", Map.of("k", "v"));

        assertThat(result).isInstanceOf(ToolResult.Pending.class);
        assertThat(skill.executions()).isZero();
    }

    @Test
    void cancelledRunStopsBeforeToolExecution() {
        TestSkillRegistry skill = new TestSkillRegistry(false);
        OpsRunService opsRunService = new OpsRunService(new ObjectMapper());
        MasterRegistry masterRegistry = masterRegistry(opsRunService, skill);
        String runId = opsRunService.create(RouteRequest.text("test")).runId();
        opsRunService.cancel(runId);
        OpsRunContextHolder.set(runId);
        try {
            ToolResult result = masterRegistry.execute("test_skill", "test_tool", Map.of());

            assertThat(result).isInstanceOf(ToolResult.Error.class);
            assertThat(result.toMap().get("message").toString()).contains("run 已被用户暂停");
            assertThat(skill.executions()).isZero();
        } finally {
            OpsRunContextHolder.clear();
        }
    }

    private static MasterRegistry masterRegistry(TestSkillRegistry skill) {
        return masterRegistry(new OpsRunService(new ObjectMapper()), skill);
    }

    private static MasterRegistry masterRegistry(OpsRunService opsRunService, TestSkillRegistry skill) {
        ApprovalService approvalService = new ApprovalService(List.of(skill), opsRunService);
        ToolExecutionRuleFilterFactory factory = new ToolExecutionRuleFilterFactory();
        var chain = factory.toolExecutionRuleFilter(List.of(
                new ToolExecuteRuleFilter(opsRunService),
                new ToolWhitelistRuleFilter(),
                new ToolTraceStartRuleFilter(opsRunService),
                new SkillResolveRuleFilter(),
                new RunCancelRuleFilter(opsRunService),
                new ToolApprovalRuleFilter(approvalService, opsRunService)));
        return new MasterRegistry(List.of(skill), chain);
    }

    private static class TestSkillRegistry implements OpsSkillRegistry {

        private final boolean approvalRequired;
        private final AtomicInteger executions = new AtomicInteger();

        private TestSkillRegistry(boolean approvalRequired) {
            this.approvalRequired = approvalRequired;
        }

        @Override
        public String name() {
            return "test_skill";
        }

        @Override
        public String description() {
            return "test";
        }

        @Override
        public String promptFragment() {
            return "test_tool";
        }

        @Override
        public Set<String> toolNames() {
            return Set.of("test_tool");
        }

        @Override
        public ToolResult execute(String toolName, Map<String, Object> args) {
            executions.incrementAndGet();
            return ToolResult.ok("executed", args);
        }

        @Override
        public boolean requiresApproval(String toolName) {
            return approvalRequired;
        }

        int executions() {
            return executions.get();
        }
    }
}
