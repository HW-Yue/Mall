package com.yue.opsagent.springai.agent.registry.rule;

import com.yue.opsagent.springai.agent.registry.AgentToolCommand;
import com.yue.opsagent.springai.agent.registry.AgentToolContext;
import com.yue.opsagent.springai.agent.registry.AgentToolRuleFilter;
import com.yue.opsagent.springai.agent.sub.ISubAgent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class AgentResolveRuleFilter implements AgentToolRuleFilter {

    @Override
    public String apply(AgentToolCommand command, AgentToolContext context) {
        ISubAgent agent = context.getAgents().get(command.agentToolName());
        if (agent == null) {
            String message = "unknown sub agent tool: " + command.agentToolName();
            context.setPhase("resolve");
            context.setOutcome("error");
            context.setErrorMessage(message);
            context.setResult(message);
            return next(command, context);
        }
        context.setAgent(agent);
        return next(command, context);
    }
}
