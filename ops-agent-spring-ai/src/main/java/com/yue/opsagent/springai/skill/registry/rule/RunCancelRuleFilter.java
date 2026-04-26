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
@Order(40)
public class RunCancelRuleFilter implements ToolExecutionRuleFilter {

    private final OpsRunService opsRunService;

    public RunCancelRuleFilter(OpsRunService opsRunService) {
        this.opsRunService = opsRunService;
    }

    @Override
    public ToolResult apply(ToolExecutionCommand command, ToolExecutionContext context) {
        if (context.hasRunId() && opsRunService.isCancelled(context.getRunId())) {
            ActiveSpan.tag("tool.cancelled", "true");
            ToolResult result = ToolResult.error("run 已被用户暂停，跳过工具调用: "
                    + command.skillName() + "." + command.toolName());
            context.setResult(result);
            opsRunService.toolEnd(context.getRunId(), command.skillName(), command.toolName(), result.toMap());
            return stop(command, context, result);
        }
        return next(command, context);
    }
}
