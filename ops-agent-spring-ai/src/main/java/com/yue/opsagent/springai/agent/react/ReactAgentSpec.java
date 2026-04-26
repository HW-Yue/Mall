package com.yue.opsagent.springai.agent.react;

import java.util.List;
import java.util.Map;

public record ReactAgentSpec(
        String agentName,
        String traceName,
        String systemPrompt,
        String userMessage,
        Map<String, Object> context,
        List<ReactTool> tools,
        int maxIters) {
}
