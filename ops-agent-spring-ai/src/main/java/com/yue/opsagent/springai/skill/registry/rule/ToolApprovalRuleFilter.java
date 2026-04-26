package com.yue.opsagent.springai.skill.registry.rule;

import com.yue.opsagent.springai.service.ApprovalService;
import com.yue.opsagent.springai.service.OpsRunService;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.registry.ToolExecutionCommand;
import com.yue.opsagent.springai.skill.registry.ToolExecutionContext;
import com.yue.opsagent.springai.skill.registry.ToolExecutionRuleFilter;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(50)
public class ToolApprovalRuleFilter implements ToolExecutionRuleFilter {

    private final ApprovalService approvalService;
    private final OpsRunService opsRunService;

    public ToolApprovalRuleFilter(ApprovalService approvalService, OpsRunService opsRunService) {
        this.approvalService = approvalService;
        this.opsRunService = opsRunService;
    }

    @Override
    public ToolResult apply(ToolExecutionCommand command, ToolExecutionContext context) {
        if (!context.getRegistry().requiresApproval(command.toolName())) {
            return next(command, context);
        }
        ToolResult result = approvalService.enqueue(
                context.getRunId(),
                command.skillName(),
                command.toolName(),
                command.args());
        context.setResult(result);
        ActiveSpan.tag("tool.status", result.getClass().getSimpleName());
        if (context.hasRunId()) {
            opsRunService.toolEnd(context.getRunId(), command.skillName(), command.toolName(), result.toMap());
        }
        return stop(command, context, result);
    }
}
