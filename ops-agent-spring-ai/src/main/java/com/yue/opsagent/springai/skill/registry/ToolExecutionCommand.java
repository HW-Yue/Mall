package com.yue.opsagent.springai.skill.registry;

import java.util.Map;

/**
 * One tool call request flowing through the tool execution rule chain.
 */
public record ToolExecutionCommand(
        String skillName,
        String toolName,
        Map<String, Object> args
) {
    public ToolExecutionCommand {
        args = args == null ? Map.of() : Map.copyOf(args);
    }
}
