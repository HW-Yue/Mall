package com.yue.opsagent.springai.domain.approval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public final class ApprovalPresenter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private ApprovalPresenter() {
    }

    public static Map<String, Object> toTaskView(ApprovalTicket ticket) {
        Map<String, Object> args = ticket.args() == null ? Map.of() : ticket.args();
        Map<String, Object> out = new HashMap<>();
        out.put("id", ticket.id());
        out.put("runId", ticket.runId());
        out.put("skillName", ticket.skillName());
        out.put("toolName", ticket.toolName());
        out.put("args", args);
        out.put("status", ticket.status());
        out.put("createdAt", ticket.createdAt());
        out.put("decidedAt", ticket.decidedAt());
        out.put("outcomeMessage", ticket.outcomeMessage());
        out.put("dataId", args.getOrDefault("dataId", ""));
        out.put("group", args.getOrDefault("group", args.getOrDefault("groupName", "")));
        out.put("reasoning", buildReasoning(ticket, args));
        out.put("errorMessage", ticket.status() == ApprovalTicket.ApprovalStatus.FAILED ? ticket.outcomeMessage() : "");
        return out;
    }

    public static String toJson(ApprovalTicket ticket) {
        try {
            return OBJECT_MAPPER.writeValueAsString(toTaskView(ticket));
        } catch (JsonProcessingException e) {
            return "{\"id\":\"" + escape(ticket.id()) + "\",\"status\":\"" + ticket.status() + "\"}";
        }
    }

    private static String buildReasoning(ApprovalTicket ticket, Map<String, Object> args) {
        StringBuilder sb = new StringBuilder();
        sb.append(ticket.skillName()).append('.').append(ticket.toolName());
        if (!args.isEmpty()) {
            sb.append(" 参数: ").append(args);
        }
        if (ticket.outcomeMessage() != null && !ticket.outcomeMessage().isBlank()) {
            sb.append("\n结果: ").append(ticket.outcomeMessage());
        }
        return sb.toString();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
