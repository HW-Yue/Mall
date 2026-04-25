package com.yue.opsagent.springai.skill.registry;

import com.yue.opsagent.springai.domain.opsroute.OpsRunContextHolder;
import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates skill menus and routes tool execution (with approval gate).
 */
@Component
public class MasterRegistry {

    private final Map<String, OpsSkillRegistry> skills;
    private final List<ToolInvocationAdvisor> advisors;

    public MasterRegistry(
            Collection<OpsSkillRegistry> registries,
            List<ToolInvocationAdvisor> advisors) {
        this.skills = registries.stream().collect(Collectors.toMap(OpsSkillRegistry::name, r -> r));
        this.advisors = List.copyOf(advisors);
    }

    /** First-round menu for the model. */
    public String buildMenu() {
        return skills.values().stream()
                .map(s -> "- " + s.name() + ": " + s.description())
                .collect(Collectors.joining("\n"));
    }

    public String promptFragment(String skillName) {
        OpsSkillRegistry s = skills.get(skillName);
        if (s == null) {
            return "未知 skill: " + skillName;
        }
        return s.promptFragment();
    }

    public ToolResult execute(String skillName, String toolName, Map<String, Object> args) {
        String runId = OpsRunContextHolder.get();
        OpsSkillRegistry reg = skills.get(skillName);
        if (reg == null) {
            return ToolResult.error("unknown skill: " + skillName);
        }
        if (!reg.toolNames().contains(toolName)) {
            return ToolResult.error("unknown tool for skill " + skillName + ": " + toolName);
        }
        ToolInvocation invocation = new ToolInvocation(runId, skillName, toolName, args, reg);
        return proceed(0, invocation);
    }

    public Map<String, OpsSkillRegistry> skills() {
        return Map.copyOf(skills);
    }

    private ToolResult proceed(int index, ToolInvocation invocation) {
        if (index >= advisors.size()) {
            return invocation.registry().execute(invocation.toolName(), invocation.args());
        }
        return advisors.get(index).invoke(invocation, next -> proceed(index + 1, next));
    }
}
