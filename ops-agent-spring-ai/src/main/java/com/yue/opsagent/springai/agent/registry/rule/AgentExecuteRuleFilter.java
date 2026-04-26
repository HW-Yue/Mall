package com.yue.opsagent.springai.agent.registry.rule;

import com.yue.opsagent.springai.agent.registry.AgentToolCommand;
import com.yue.opsagent.springai.agent.registry.AgentToolContext;
import com.yue.opsagent.springai.agent.registry.AgentToolRuleFilter;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class AgentExecuteRuleFilter implements AgentToolRuleFilter {

    @Override
    public String apply(AgentToolCommand command, AgentToolContext context) {
        if (context.hasResult()) {
            return next(command, context);
        }
        try {
            String result = context.getAgent().runReact(command.task(), command.context());
            context.setPhase("execute");
            context.setOutcome("success");
            context.setResult(result == null ? "" : result);
        } catch (RuntimeException e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            ActiveSpan.error(message);
            context.setPhase("execute");
            context.setOutcome("error");
            context.setErrorMessage(message);
            context.setResult("子Agent执行失败: " + message);
        }
        return next(command, context);
    }
}
