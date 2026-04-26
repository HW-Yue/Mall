package com.yue.opsagent.springai.skill.registry;

import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import com.yue.opsagent.springai.skill.api.ToolResult;

/**
 * Extension point for tool execution governance rules.
 */
public interface ToolExecutionRuleFilter
        extends ILogicHandler<ToolExecutionCommand, ToolExecutionContext, ToolResult> {
}
