package com.yue.opsagent.springai.skill.registry;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;

import java.util.Map;

public record ToolInvocation(
        String runId,
        String skillName,
        String toolName,
        Map<String, Object> args,
        OpsSkillRegistry registry
) {
    public ToolInvocation {
        args = args == null ? Map.of() : Map.copyOf(args);
    }
}
