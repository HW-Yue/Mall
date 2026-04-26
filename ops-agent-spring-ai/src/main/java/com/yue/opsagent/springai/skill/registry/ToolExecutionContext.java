package com.yue.opsagent.springai.skill.registry;

import cn.bugstack.wrench.design.framework.link.model2.DynamicContext;
import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Mutable state shared by tool execution rule filters.
 */
public class ToolExecutionContext extends DynamicContext {

    private final String runId;
    private final Map<String, OpsSkillRegistry> skills;
    private final Instant startedAt = Instant.now();
    private OpsSkillRegistry registry;
    private ToolResult result;
    private String phase;
    private String outcome;
    private String errorMessage;

    public ToolExecutionContext(String runId, Map<String, OpsSkillRegistry> skills) {
        this.runId = runId;
        this.skills = Map.copyOf(skills);
    }

    public String getRunId() {
        return runId;
    }

    public Map<String, OpsSkillRegistry> getSkills() {
        return skills;
    }

    public OpsSkillRegistry getRegistry() {
        return registry;
    }

    public void setRegistry(OpsSkillRegistry registry) {
        this.registry = registry;
    }

    public ToolResult getResult() {
        return result;
    }

    public void setResult(ToolResult result) {
        this.result = result;
    }

    public boolean hasResult() {
        return result != null;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long durationMs() {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }

    public boolean hasRunId() {
        return runId != null && !runId.isBlank();
    }
}
