package com.yue.opsagent.springai.service;

import com.yue.opsagent.springai.domain.alert.AlertEvent;
import com.yue.opsagent.springai.domain.alert.AlertPlaceholderResolver;
import com.yue.opsagent.springai.agent.registry.AgentToolRegistry;
import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.infrastructure.observability.OpsLogFormatter;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.registry.MasterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes SOP steps: {@code direct_tool} or {@code delegate_subagent}.
 */
@Component
public class SopStepRunner {

    private static final Logger log = LoggerFactory.getLogger(SopStepRunner.class);

    private final MasterRegistry masterRegistry;
    private final AgentToolRegistry agentToolRegistry;
    private final ObjectMapper objectMapper;

    public SopStepRunner(
            MasterRegistry masterRegistry,
            AgentToolRegistry agentToolRegistry,
            ObjectMapper objectMapper) {
        this.masterRegistry = masterRegistry;
        this.agentToolRegistry = agentToolRegistry;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> run(AlertEvent event, List<OpsAiProperties.Sop.Step> steps) {
        log.info("[SopStepRunner] deterministic 开始 alertname={} application={} severity={} labels={} annotations={} steps={}",
                event.alertname(),
                event.application(),
                event.severity(),
                event.labels(),
                event.annotations(),
                steps == null ? 0 : steps.size());
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, String> flat = AlertPlaceholderResolver.flattenLabels(event);
        for (OpsAiProperties.Sop.Step step : steps) {
            try {
                out.add(executeStep(event, step, flat));
            } catch (Exception ex) {
                log.warn("[SopStepRunner] step failed: {}", ex.toString());
                out.add(Map.of(
                        "type", stepType(step),
                        "error", ex.getMessage() == null ? "unknown" : ex.getMessage()));
                if (!"continue".equalsIgnoreCase(step.getOnError())) {
                    break;
                }
            }
        }
        try {
            log.info("[SopStepRunner] deterministic 结束 alertname={} 步骤执行结果 JSON（截断）↓↓\n{}",
                    event.alertname(),
                    OpsLogFormatter.truncate(objectMapper.writeValueAsString(out), OpsLogFormatter.ALERT_JSON_MAX));
        } catch (JsonProcessingException e) {
            log.warn("[SopStepRunner] 结果序列化失败 alertname={} err={}", event.alertname(), e.toString());
        }
        return out;
    }

    private Map<String, Object> executeStep(AlertEvent event, OpsAiProperties.Sop.Step step, Map<String, String> flat) {
        String type = step.getType() == null ? "direct_tool" : step.getType().trim();
        if ("delegate_subagent".equalsIgnoreCase(type)) {
            String task = AlertPlaceholderResolver.substituteTemplate(step.getTask(), flat);
            Map<String, Object> ctx = new HashMap<>(AlertPlaceholderResolver.resolveArgs(step.getContext(), flat));
            ctx.put("alert", Map.of(
                    "alertname", AlertPlaceholderResolver.nullToEmpty(event.alertname()),
                    "severity", AlertPlaceholderResolver.nullToEmpty(event.severity()),
                    "application", AlertPlaceholderResolver.nullToEmpty(event.application()),
                    "labels", event.labels() == null ? Map.of() : event.labels(),
                    "annotations", event.annotations() == null ? Map.of() : event.annotations()));
            String summary = agentToolRegistry.execute(step.getSubAgentId(), task, ctx);
            return Map.of(
                    "type", "delegate_subagent",
                    "subAgentId", step.getSubAgentId(),
                    "task", task,
                    "summary", summary);
        }
        // direct_tool (default): infer from legacy steps without explicit type
        Map<String, Object> args = AlertPlaceholderResolver.resolveArgs(step.getArgs(), flat);
        ToolResult r = masterRegistry.execute(step.getSkill(), step.getTool(), args);
        return Map.of(
                "type", "direct_tool",
                "skill", step.getSkill(),
                "tool", step.getTool(),
                "args", args,
                "result", r.toMap());
    }

    private static String stepType(OpsAiProperties.Sop.Step step) {
        return step.getType() == null ? "direct_tool" : step.getType();
    }
}
