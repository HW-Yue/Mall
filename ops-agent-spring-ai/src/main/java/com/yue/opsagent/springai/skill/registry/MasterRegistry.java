package com.yue.opsagent.springai.skill.registry;

import cn.bugstack.wrench.design.framework.link.model2.chain.BusinessLinkedList;
import com.yue.opsagent.springai.domain.opsroute.OpsRunContextHolder;
import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates skill menus and routes tool execution through the Wrench rule chain.
 */
@Component
public class MasterRegistry {

    private final Map<String, OpsSkillRegistry> skills;
    private final BusinessLinkedList<ToolExecutionCommand, ToolExecutionContext, ToolResult> toolExecutionRuleFilter;

    public MasterRegistry(
            Collection<OpsSkillRegistry> registries,
            BusinessLinkedList<ToolExecutionCommand, ToolExecutionContext, ToolResult> toolExecutionRuleFilter) {
        this.skills = registries.stream().collect(Collectors.toMap(OpsSkillRegistry::name, r -> r));
        this.toolExecutionRuleFilter = toolExecutionRuleFilter;
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

    @Trace(operationName = "tool.execute")
    public ToolResult execute(String skillName, String toolName, Map<String, Object> args) {
        String runId = OpsRunContextHolder.get();
        ToolExecutionCommand command = new ToolExecutionCommand(skillName, toolName, args);
        ToolExecutionContext context = new ToolExecutionContext(runId, skills);
        try {
            return toolExecutionRuleFilter.apply(command, context);
        } catch (Exception e) {
            return ToolResult.error(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    public Map<String, OpsSkillRegistry> skills() {
        return Map.copyOf(skills);
    }
}
