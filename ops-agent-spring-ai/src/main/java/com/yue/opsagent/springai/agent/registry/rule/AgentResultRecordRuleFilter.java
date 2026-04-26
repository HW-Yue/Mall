package com.yue.opsagent.springai.agent.registry.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.agent.registry.AgentToolCommand;
import com.yue.opsagent.springai.agent.registry.AgentToolContext;
import com.yue.opsagent.springai.agent.registry.AgentToolRuleFilter;
import com.yue.opsagent.springai.service.OpsRunService;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(200)
public class AgentResultRecordRuleFilter implements AgentToolRuleFilter {

    private static final Logger log = LoggerFactory.getLogger(AgentResultRecordRuleFilter.class);

    private final OpsRunService opsRunService;
    private final ObjectMapper objectMapper;

    public AgentResultRecordRuleFilter(OpsRunService opsRunService, ObjectMapper objectMapper) {
        this.opsRunService = opsRunService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String apply(AgentToolCommand command, AgentToolContext context) {
        String result = context.getResult();
        if (result == null) {
            result = "sub agent chain completed without result";
            context.setResult(result);
            context.setPhase("record");
            context.setOutcome("error");
            context.setErrorMessage(result);
        }
        Map<String, Object> event = buildEvent(command, context, result);
        ActiveSpan.tag("sub_agent.status", context.getOutcome() == null ? "unknown" : context.getOutcome());
        if (context.getErrorMessage() != null && !context.getErrorMessage().isBlank()) {
            ActiveSpan.error(context.getErrorMessage());
        }
        if (context.hasRunId()) {
            opsRunService.subAgentResult(context.getRunId(), command.agentToolName(), event);
        }
        log.info("[SubAgentResult] {}", toJson(event));
        return stop(command, context, result);
    }

    private Map<String, Object> buildEvent(AgentToolCommand command, AgentToolContext context, String result) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", "sub_agent_result");
        event.put("runId", context.getRunId() == null ? "" : context.getRunId());
        event.put("agent", context.getAgent() == null ? command.agentToolName() : context.getAgent().parentDisplayName());
        event.put("agentTool", command.agentToolName() == null ? "" : command.agentToolName());
        event.put("task", command.task());
        event.put("phase", context.getPhase() == null ? "unknown" : context.getPhase());
        event.put("outcome", context.getOutcome() == null ? "unknown" : context.getOutcome());
        event.put("result", result);
        event.put("durationMs", context.durationMs());
        return event;
    }

    private String toJson(Map<String, Object> event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            return event.toString();
        }
    }
}
