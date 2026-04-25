package com.yue.opsagent.springai.skill.api;

import java.util.Map;

@FunctionalInterface
public interface ToolExecutor {
    ToolResult execute(Map<String, Object> args);
}
