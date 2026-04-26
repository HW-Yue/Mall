package com.yue.opsagent.springai.skill.registry.rule;

import com.yue.opsagent.springai.service.OpsRunService;
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

    private final OpsRunService opsRunService;

    public ToolExecuteRuleFilter(OpsRunService opsRunService) {
        this.opsRunService = opsRunService;
    }

    @Override
    public ToolResult apply(ToolExecutionCommand command, ToolExecutionContext context) {
        ToolResult result;
        try {
            result = context.getRegistry().execute(command.toolName(), command.args());
        } catch (RuntimeException e) {
            ActiveSpan.error(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            ActiveSpan.tag("tool.status", "ERROR");
            throw e;
        }
        context.setResult(result);
        ActiveSpan.tag("tool.status", result.getClass().getSimpleName());
        if (context.hasRunId()) {
            opsRunService.toolEnd(context.getRunId(), command.skillName(), command.toolName(), result.toMap());
        }
        return stop(command, context, result);
    }
}
