package com.yue.opsagent.springai.agent.react;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ReactActionParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ReactActionParser() {}

    public static Optional<ParsedAction> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String json = stripMarkdownFence(raw.trim());
        try {
            JsonNode n = MAPPER.readTree(json);
            if (!n.has("action")) {
                return Optional.empty();
            }
            String action = n.get("action").asText();
            if ("FINAL".equalsIgnoreCase(action)) {
                String answer = n.has("answer") ? n.get("answer").asText("") : "";
                return Optional.of(new ParsedAction.FinalAction(answer));
            }
            if ("CALL_TOOL".equalsIgnoreCase(action)) {
                String tool = n.has("tool") ? n.get("tool").asText("") : "";
                Map<String, Object> args = new HashMap<>();
                if (n.has("args") && n.get("args").isObject()) {
                    n.get("args").properties().forEach(e -> args.put(e.getKey(), jsonNodeToValue(e.getValue())));
                }
                return Optional.of(new ParsedAction.CallTool(tool, args));
            }
        } catch (JsonProcessingException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    public static String jsonFormatReminder() {
        return "请只输出 JSON：{\"action\":\"CALL_TOOL\",\"tool\":\"...\",\"args\":{...}} 或 {\"action\":\"FINAL\",\"answer\":\"...\"}";
    }

    private static Object jsonNodeToValue(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isTextual()) {
            return n.asText();
        }
        if (n.isInt()) {
            return n.intValue();
        }
        if (n.isLong()) {
            return n.longValue();
        }
        if (n.isDouble() || n.isFloat()) {
            return n.doubleValue();
        }
        if (n.isBoolean()) {
            return n.booleanValue();
        }
        return n.toString();
    }

    private static String stripMarkdownFence(String s) {
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl > 0) {
                s = s.substring(nl + 1);
            }
            int end = s.lastIndexOf("```");
            if (end > 0) {
                s = s.substring(0, end);
            }
        }
        return s.trim();
    }

    public sealed interface ParsedAction permits ParsedAction.CallTool, ParsedAction.FinalAction {

        record CallTool(String tool, Map<String, Object> args) implements ParsedAction {}

        record FinalAction(String answer) implements ParsedAction {}
    }
}
