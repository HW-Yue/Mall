package com.yue.opsagent.springai.skill.registry.rule;

import com.yue.opsagent.springai.service.ApprovalService;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.registry.ToolExecutionCommand;
import com.yue.opsagent.springai.skill.registry.ToolExecutionContext;
import com.yue.opsagent.springai.skill.registry.ToolExecutionRuleFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(50)
public class ToolApprovalRuleFilter implements ToolExecutionRuleFilter {

    private final ApprovalService approvalService;

    public ToolApprovalRuleFilter(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @Override
    public ToolResult apply(ToolExecutionCommand command, ToolExecutionContext context) {
        if (context.hasResult()) {
            return next(command, context);
        }
        if (!context.getRegistry().requiresApproval(command.toolName())) {
            return next(command, context);
        }
        ToolResult result = approvalService.enqueue(
                context.getRunId(),
                command.skillName(),
                command.toolName(),
                command.args());
        context.setResult(result);
        context.setPhase("approval");
        context.setOutcome("pending_approval");
        return next(command, context);
    }
}
