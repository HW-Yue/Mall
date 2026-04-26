package com.yue.opsagent.springai.skill.registry.rule;

import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.registry.ToolExecutionCommand;
import com.yue.opsagent.springai.skill.registry.ToolExecutionContext;
import com.yue.opsagent.springai.skill.registry.ToolExecutionRuleFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class ToolWhitelistRuleFilter implements ToolExecutionRuleFilter {

    @Override
    public ToolResult apply(ToolExecutionCommand command, ToolExecutionContext context) {
        if (!context.getRegistry().toolNames().contains(command.toolName())) {
            return stop(command, context, ToolResult.error(
                    "unknown tool for skill " + command.skillName() + ": " + command.toolName()));
        }
        return next(command, context);
    }
}
