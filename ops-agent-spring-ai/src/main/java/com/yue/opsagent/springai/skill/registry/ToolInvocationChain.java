package com.yue.opsagent.springai.skill.registry;

import com.yue.opsagent.springai.skill.api.ToolResult;

@FunctionalInterface
public interface ToolInvocationChain {

    ToolResult proceed(ToolInvocation invocation);
}
