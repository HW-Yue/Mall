package com.yue.opsagent.springai.skill.registry.rule;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.registry.ToolExecutionCommand;
import com.yue.opsagent.springai.skill.registry.ToolExecutionContext;
import com.yue.opsagent.springai.skill.registry.ToolExecutionRuleFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class SkillResolveRuleFilter implements ToolExecutionRuleFilter {

    @Override
    public ToolResult apply(ToolExecutionCommand command, ToolExecutionContext context) {
        OpsSkillRegistry registry = context.getSkills().get(command.skillName());
        if (registry == null) {
            return stop(command, context, ToolResult.error("unknown skill: " + command.skillName()));
        }
        context.setRegistry(registry);
        return next(command, context);
    }
}
