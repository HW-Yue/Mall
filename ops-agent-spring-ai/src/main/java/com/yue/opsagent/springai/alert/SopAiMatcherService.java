package com.yue.opsagent.springai.alert;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.config.OpsAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Uses the LLM only as a fuzzy selector over existing SOP rules.
 */
@Service
public class SopAiMatcherService {

    private static final Logger log = LoggerFactory.getLogger(SopAiMatcherService.class);
    private static final double MIN_CONFIDENCE = 0.35d;
    private static final int SOP_SNIPPET_LIMIT = 900;

    private final OpsAiProperties opsAiProperties;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public SopAiMatcherService(
            OpsAiProperties opsAiProperties,
            ChatClient.Builder chatClientBuilder,
            ObjectMapper objectMapper) {
        this.opsAiProperties = opsAiProperties;
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public Optional<MatchResult> matchEvent(AlertEvent event) {
        String text = """
                status=%s
                alertname=%s
                severity=%s
                application=%s
                labels=%s
                annotations=%s
                """.formatted(
                AlertPlaceholderResolver.nullToEmpty(event.status()),
                AlertPlaceholderResolver.nullToEmpty(event.alertname()),
                AlertPlaceholderResolver.nullToEmpty(event.severity()),
                AlertPlaceholderResolver.nullToEmpty(event.application()),
                event.labels() == null ? Map.of() : event.labels(),
                event.annotations() == null ? Map.of() : event.annotations());
        return matchText(text);
    }

    public Optional<MatchResult> matchText(String text) {
        List<OpsAiProperties.Sop.Rule> rules = opsAiProperties.getSop().getRules();
        if (rules == null || rules.isEmpty() || text == null || text.isBlank()) {
            return Optional.empty();
        }
        String answer;
        try {
            answer = chatClient.prompt()
                    .system(systemPrompt())
                    .user(buildUserPrompt(text, rules))
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("[SopAiMatcher] LLM fuzzy match failed: {}", e.toString());
            return Optional.empty();
        }
        return parseAnswer(answer, rules);
    }

    private Optional<MatchResult> parseAnswer(String answer, List<OpsAiProperties.Sop.Rule> rules) {
        try {
            Map<String, Object> raw = objectMapper.readValue(extractJson(answer), new TypeReference<>() {
            });
            int index = intValue(raw.get("index"), -1);
            double confidence = doubleValue(raw.get("confidence"), 1d);
            String reason = raw.get("reason") == null ? "" : String.valueOf(raw.get("reason"));
            if (index < 0 || index >= rules.size()) {
                return Optional.empty();
            }
            if (confidence < MIN_CONFIDENCE) {
                log.info("[SopAiMatcher] fuzzy match rejected index={} confidence={} reason={}",
                        index, confidence, reason);
                return Optional.empty();
            }
            return Optional.of(new MatchResult(index, confidence, reason, rules.get(index)));
        } catch (Exception e) {
            log.warn("[SopAiMatcher] cannot parse LLM match answer: answer={} err={}", answer, e.toString());
            return Optional.empty();
        }
    }

    private static String systemPrompt() {
        return """
                你是 SOP 规则选择器。你的任务不是处理告警，而是从候选 SOP 中选择最匹配的一条。
                只能选择候选列表里的 index；不能创造新 SOP；如果没有合适候选，返回 index=-1。
                只返回 JSON，不要 Markdown，不要解释。
                JSON 格式：{"index":0,"confidence":0.0,"reason":"简短中文原因"}
                confidence 范围 0 到 1。
                """;
    }

    private static String buildUserPrompt(String text, List<OpsAiProperties.Sop.Rule> rules) {
        StringBuilder sb = new StringBuilder();
        sb.append("输入文本：\n").append(text).append("\n\n候选 SOP：\n");
        for (int i = 0; i < rules.size(); i++) {
            OpsAiProperties.Sop.Rule r = rules.get(i);
            sb.append("index=").append(i).append('\n');
            sb.append("matchAlertname=").append(nullToEmpty(r.getMatchAlertname())).append('\n');
            sb.append("matchCategory=").append(nullToEmpty(r.getMatchCategory())).append('\n');
            sb.append("matchSeverity=").append(nullToEmpty(r.getMatchSeverity())).append('\n');
            sb.append("matchApplication=").append(nullToEmpty(r.getMatchApplication())).append('\n');
            sb.append("sop=").append(snippet(r.getSopMarkdown())).append("\n---\n");
        }
        return sb.toString();
    }

    private static String extractJson(String answer) {
        if (answer == null) {
            return "{}";
        }
        String s = answer.trim();
        if (s.startsWith("```")) {
            int firstLine = s.indexOf('\n');
            int lastFence = s.lastIndexOf("```");
            if (firstLine >= 0 && lastFence > firstLine) {
                s = s.substring(firstLine + 1, lastFence).trim();
            }
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return s;
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double doubleValue(Object value, double fallback) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String snippet(String value) {
        String s = nullToEmpty(value).replaceAll("\\s+", " ").trim();
        return s.length() <= SOP_SNIPPET_LIMIT ? s : s.substring(0, SOP_SNIPPET_LIMIT) + "...";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record MatchResult(
            int index,
            double confidence,
            String reason,
            OpsAiProperties.Sop.Rule rule) {

        public Map<String, Object> toMap() {
            Map<String, Object> out = new HashMap<>();
            out.put("matched", true);
            out.put("index", index);
            out.put("confidence", confidence);
            out.put("reason", reason);
            out.put("matchAlertname", rule.getMatchAlertname());
            out.put("matchCategory", rule.getMatchCategory());
            out.put("matchSeverity", rule.getMatchSeverity());
            out.put("matchApplication", rule.getMatchApplication());
            out.put("sopPreview", snippet(rule.getSopMarkdown()));
            return out;
        }
    }
}
