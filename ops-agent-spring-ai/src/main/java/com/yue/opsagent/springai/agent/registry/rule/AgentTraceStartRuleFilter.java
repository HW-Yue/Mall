package com.yue.opsagent.springai.agent.registry.rule;

import com.yue.opsagent.springai.agent.registry.AgentToolCommand;
import com.yue.opsagent.springai.agent.registry.AgentToolContext;
import com.yue.opsagent.springai.agent.registry.AgentToolRuleFilter;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class AgentTraceStartRuleFilter implements AgentToolRuleFilter {

    @Override
    public String apply(AgentToolCommand command, AgentToolContext context) {
        if (context.hasResult()) {
            return next(command, context);
        }
        ActiveSpan.tag("sub_agent.tool", command.agentToolName());
        ActiveSpan.tag("sub_agent.name", context.getAgent().parentDisplayName());
        return next(command, context);
    }
}
