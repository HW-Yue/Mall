package com.yue.opsagent.springai.agent.registry.rule;

import com.yue.opsagent.springai.agent.registry.AgentToolCommand;
import com.yue.opsagent.springai.agent.registry.AgentToolContext;
import com.yue.opsagent.springai.agent.registry.AgentToolRuleFilter;
import com.yue.opsagent.springai.service.OpsRunService;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
public class AgentCancelRuleFilter implements AgentToolRuleFilter {

    private final OpsRunService opsRunService;

    public AgentCancelRuleFilter(OpsRunService opsRunService) {
        this.opsRunService = opsRunService;
    }

    @Override
    public String apply(AgentToolCommand command, AgentToolContext context) {
        if (context.hasResult()) {
            return next(command, context);
        }
        if (context.hasRunId() && opsRunService.isCancelled(context.getRunId())) {
            String message = "run 已被用户暂停，跳过子Agent委派: " + command.agentToolName();
            ActiveSpan.tag("sub_agent.cancelled", "true");
            context.setPhase("cancel");
            context.setOutcome("cancelled");
            context.setErrorMessage(message);
            context.setResult(message);
        }
        return next(command, context);
    }
}
