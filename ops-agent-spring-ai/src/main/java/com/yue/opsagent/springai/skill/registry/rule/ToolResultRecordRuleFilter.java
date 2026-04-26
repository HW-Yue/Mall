package com.yue.opsagent.springai.skill.registry.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.service.OpsRunService;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.registry.ToolExecutionCommand;
import com.yue.opsagent.springai.skill.registry.ToolExecutionContext;
import com.yue.opsagent.springai.skill.registry.ToolExecutionRuleFilter;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(200)
public class ToolResultRecordRuleFilter implements ToolExecutionRuleFilter {

    private static final Logger log = LoggerFactory.getLogger(ToolResultRecordRuleFilter.class);

    private final OpsRunService opsRunService;
    private final ObjectMapper objectMapper;

    public ToolResultRecordRuleFilter(OpsRunService opsRunService, ObjectMapper objectMapper) {
        this.opsRunService = opsRunService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolResult apply(ToolExecutionCommand command, ToolExecutionContext context) {
        ToolResult result = context.getResult();
        if (result == null) {
            result = ToolResult.error("tool execution chain completed without result");
            context.setResult(result);
            context.setPhase("record");
            context.setOutcome("error");
            context.setErrorMessage("tool execution chain completed without result");
        }

        Map<String, Object> event = buildEvent(command, context, result);
        ActiveSpan.tag("tool.status", result.getClass().getSimpleName());
        if (context.getErrorMessage() != null && !context.getErrorMessage().isBlank()) {
            ActiveSpan.error(context.getErrorMessage());
        }
        if (context.hasRunId()) {
            opsRunService.toolResult(context.getRunId(), command.skillName(), command.toolName(), event);
        }
        log.info("[ToolResult] {}", toJson(event));
        return stop(command, context, result);
    }

    private Map<String, Object> buildEvent(
            ToolExecutionCommand command,
            ToolExecutionContext context,
            ToolResult result) {
        Map<String, Object> resultMap = result.toMap();
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", "tool_result");
        event.put("runId", context.getRunId() == null ? "" : context.getRunId());
        event.put("skill", command.skillName() == null ? "" : command.skillName());
        event.put("tool", command.toolName() == null ? "" : command.toolName());
        event.put("args", command.args() == null ? Map.of() : command.args());
        event.put("phase", context.getPhase() == null ? "unknown" : context.getPhase());
        event.put("outcome", context.getOutcome() == null ? inferOutcome(result) : context.getOutcome());
        event.put("status", String.valueOf(resultMap.getOrDefault("status", result.getClass().getSimpleName())));
        event.put("message", String.valueOf(resultMap.getOrDefault("message", "")));
        event.put("result", resultMap);
        event.put("durationMs", context.durationMs());
        return event;
    }

    private static String inferOutcome(ToolResult result) {
        if (result instanceof ToolResult.Pending) {
            return "pending_approval";
        }
        if (result instanceof ToolResult.Error) {
            return "error";
        }
        return "success";
    }

    private String toJson(Map<String, Object> event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            return event.toString();
        }
    }
}
