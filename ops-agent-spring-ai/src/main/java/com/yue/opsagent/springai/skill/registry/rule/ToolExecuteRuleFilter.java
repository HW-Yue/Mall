package com.yue.opsagent.springai.skill.registry.rule;

import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.registry.ToolExecutionCommand;
import com.yue.opsagent.springai.skill.registry.ToolExecutionContext;
import com.yue.opsagent.springai.skill.registry.ToolExecutionRuleFilter;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class ToolExecuteRuleFilter implements ToolExecutionRuleFilter {

    @Override
    public ToolResult apply(ToolExecutionCommand command, ToolExecutionContext context) {
        if (context.hasResult()) {
            return next(command, context);
        }
        ToolResult result;
        try {
            result = context.getRegistry().execute(command.toolName(), command.args());
        } catch (RuntimeException e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            ActiveSpan.error(message);
            context.setPhase("execute");
            context.setOutcome("error");
            context.setErrorMessage(message);
            context.setResult(ToolResult.error(message));
            return next(command, context);
        }
        context.setResult(result);
        context.setPhase("execute");
        context.setOutcome(result instanceof ToolResult.Error ? "error" : "success");
        return next(command, context);
    }
}
