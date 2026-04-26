package com.yue.opsagent.springai.skill.registry;

import cn.bugstack.wrench.design.framework.link.model2.DynamicContext;
import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;

import java.util.Map;

/**
 * Mutable state shared by tool execution rule filters.
 */
public class ToolExecutionContext extends DynamicContext {

    private final String runId;
    private final Map<String, OpsSkillRegistry> skills;
    private OpsSkillRegistry registry;
    private ToolResult result;

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

    public boolean hasRunId() {
        return runId != null && !runId.isBlank();
    }
}
