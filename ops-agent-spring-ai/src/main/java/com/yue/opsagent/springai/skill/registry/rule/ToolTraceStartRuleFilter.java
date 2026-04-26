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
@Order(30)
public class ToolTraceStartRuleFilter implements ToolExecutionRuleFilter {

    private final OpsRunService opsRunService;

    public ToolTraceStartRuleFilter(OpsRunService opsRunService) {
        this.opsRunService = opsRunService;
    }

    @Override
    public ToolResult apply(ToolExecutionCommand command, ToolExecutionContext context) {
        if (context.hasResult()) {
            return next(command, context);
        }
        ActiveSpan.tag("tool.skill", command.skillName());
        ActiveSpan.tag("tool.name", command.toolName());
        if (context.hasRunId()) {
            opsRunService.toolStart(context.getRunId(), command.skillName(), command.toolName(), command.args());
        }
        return next(command, context);
    }
}
