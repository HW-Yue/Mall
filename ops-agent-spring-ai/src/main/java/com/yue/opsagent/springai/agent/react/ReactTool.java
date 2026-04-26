package com.yue.opsagent.springai.agent.react;

import java.util.Map;

public interface ReactTool {

    String name();

    String description();

    String execute(Map<String, Object> args, Map<String, Object> context);
}
