package com.yue.opsagent.springai.skill.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Uniform tool output for LLM context and APIs.
 */
public sealed interface ToolResult permits ToolResult.Ok, ToolResult.Error, ToolResult.Pending {

    ObjectMapper MAPPER = new ObjectMapper();

    static Ok ok(String message, Object data) {
        return new Ok(message, data);
    }

    static Ok ok(String message) {
        return new Ok(message, null);
    }

    static Error error(String message) {
        return new Error(message);
    }

    static Pending pending(String approvalId, String message) {
        return new Pending(approvalId, message);
    }

    default String toJson() {
        try {
            return MAPPER.writeValueAsString(toMap());
        } catch (JsonProcessingException e) {
            return "{\"error\":\"serialize_failed\"}";
        }
    }

    Map<String, Object> toMap();

    record Ok(String message, Object data) implements ToolResult {
        @Override
        public Map<String, Object> toMap() {
            java.util.HashMap<String, Object> m = new java.util.HashMap<>();
            m.put("status", "ok");
            m.put("message", message);
            if (data != null) {
                m.put("data", data);
            }
            return m;
        }
    }

    record Error(String message) implements ToolResult {
        @Override
        public Map<String, Object> toMap() {
            return Map.of("status", "error", "message", message);
        }
    }

    record Pending(String approvalId, String message) implements ToolResult {
        @Override
        public Map<String, Object> toMap() {
            return Map.of("status", "pending_approval", "approvalId", approvalId, "message", message);
        }
    }
}
