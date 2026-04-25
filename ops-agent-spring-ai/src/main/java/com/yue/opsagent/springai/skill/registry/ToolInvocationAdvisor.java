package com.yue.opsagent.springai.skill.registry;

import com.yue.opsagent.springai.skill.api.ToolResult;

public interface ToolInvocationAdvisor {

    ToolResult invoke(ToolInvocation invocation, ToolInvocationChain chain);
}
