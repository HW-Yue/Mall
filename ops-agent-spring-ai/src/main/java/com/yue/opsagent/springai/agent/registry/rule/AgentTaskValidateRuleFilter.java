package com.yue.opsagent.springai.agent.registry.rule;

import com.yue.opsagent.springai.agent.registry.AgentToolCommand;
import com.yue.opsagent.springai.agent.registry.AgentToolContext;
import com.yue.opsagent.springai.agent.registry.AgentToolRuleFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class AgentTaskValidateRuleFilter implements AgentToolRuleFilter {

    @Override
    public String apply(AgentToolCommand command, AgentToolContext context) {
        if (context.hasResult()) {
            return next(command, context);
        }
        if (command.task().isBlank()) {
            String message = "子Agent任务不能为空: " + command.agentToolName();
            context.setPhase("validate");
            context.setOutcome("error");
            context.setErrorMessage(message);
            context.setResult(message);
        }
        return next(command, context);
    }
}
