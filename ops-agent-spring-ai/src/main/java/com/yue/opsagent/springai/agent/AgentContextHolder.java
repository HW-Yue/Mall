package com.yue.opsagent.springai.agent;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-thread context for parent→sub delegation during a single agent call.
 */
@Component
public class AgentContextHolder {

    private final ThreadLocal<Map<String, Object>> context = new ThreadLocal<>();

    public void set(Map<String, Object> ctx) {
        context.set(ctx == null ? Map.of() : new HashMap<>(ctx));
    }

    public Map<String, Object> get() {
        Map<String, Object> m = context.get();
        return m == null ? Map.of() : Collections.unmodifiableMap(m);
    }

    public Map<String, Object> mutableCopyForSubAgent() {
        Map<String, Object> m = context.get();
        return m == null ? new HashMap<>() : new HashMap<>(m);
    }

    public void clear() {
        context.remove();
    }
}
