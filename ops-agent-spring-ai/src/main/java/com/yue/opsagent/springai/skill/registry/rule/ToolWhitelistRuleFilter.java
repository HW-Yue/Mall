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
        if (context.hasResult()) {
            return next(command, context);
        }
        if (!context.getRegistry().toolNames().contains(command.toolName())) {
            String message = "unknown tool for skill " + command.skillName() + ": " + command.toolName();
            context.setPhase("whitelist");
            context.setOutcome("error");
            context.setErrorMessage(message);
            context.setResult(ToolResult.error(message));
        }
        return next(command, context);
    }
}
