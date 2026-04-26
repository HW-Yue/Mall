package com.yue.opsagent.springai.agent.registry;

import java.util.Map;

public record AgentToolCommand(
        String agentToolName,
        String task,
        Map<String, Object> context
) {
    public AgentToolCommand {
        task = task == null ? "" : task;
        context = context == null ? Map.of() : Map.copyOf(context);
    }
}
