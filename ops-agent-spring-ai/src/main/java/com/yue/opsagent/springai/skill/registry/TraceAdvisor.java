package com.yue.opsagent.springai.skill.registry;

import com.yue.opsagent.springai.opsroute.OpsRunService;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class TraceAdvisor implements ToolInvocationAdvisor {

    private final OpsRunService opsRunService;

    public TraceAdvisor(OpsRunService opsRunService) {
        this.opsRunService = opsRunService;
    }

    @Override
    public ToolResult invoke(ToolInvocation invocation, ToolInvocationChain chain) {
        String runId = invocation.runId();
        if (runId != null && !runId.isBlank() && opsRunService.isCancelled(runId)) {
            return ToolResult.error("run 已被用户暂停，跳过工具调用: "
                    + invocation.skillName() + "." + invocation.toolName());
        }
        if (runId != null && !runId.isBlank()) {
            opsRunService.toolStart(runId, invocation.skillName(), invocation.toolName(), invocation.args());
        }
        ToolResult result = chain.proceed(invocation);
        if (runId != null && !runId.isBlank()) {
            opsRunService.toolEnd(runId, invocation.skillName(), invocation.toolName(), result.toMap());
        }
        return result;
    }
}
