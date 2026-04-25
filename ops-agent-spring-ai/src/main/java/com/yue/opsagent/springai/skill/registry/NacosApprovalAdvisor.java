package com.yue.opsagent.springai.skill.registry;

import com.yue.opsagent.springai.service.ApprovalService;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class NacosApprovalAdvisor implements ToolInvocationAdvisor {

    private final ApprovalService approvalService;

    public NacosApprovalAdvisor(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @Override
    public ToolResult invoke(ToolInvocation invocation, ToolInvocationChain chain) {
        if (invocation.registry().requiresApproval(invocation.toolName())) {
            return approvalService.enqueue(
                    invocation.runId(),
                    invocation.skillName(),
                    invocation.toolName(),
                    invocation.args());
        }
        return chain.proceed(invocation);
    }
}
