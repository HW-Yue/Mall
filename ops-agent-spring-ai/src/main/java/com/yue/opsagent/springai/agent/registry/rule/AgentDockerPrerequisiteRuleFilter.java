package com.yue.opsagent.springai.agent.registry.rule;

import com.yue.opsagent.springai.agent.registry.AgentToolCommand;
import com.yue.opsagent.springai.agent.registry.AgentToolContext;
import com.yue.opsagent.springai.agent.registry.AgentToolRuleFilter;
import com.yue.opsagent.springai.domain.opsroute.OpsRunEvent;
import com.yue.opsagent.springai.service.OpsRunService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Order(25)
public class AgentDockerPrerequisiteRuleFilter implements AgentToolRuleFilter {

    private final OpsRunService opsRunService;

    public AgentDockerPrerequisiteRuleFilter(OpsRunService opsRunService) {
        this.opsRunService = opsRunService;
    }

    @Override
    public String apply(AgentToolCommand command, AgentToolContext context) {
        if (context.hasResult()) {
            return next(command, context);
        }
        if (!"docker_skill".equals(command.agentToolName())) {
            return next(command, context);
        }
        if (!hasResolvedContainerName(context.getRunId())) {
            String message = "docker_skill 前必须先通过 catalog_describe_service 获取非空 containerName；当前 run 里还没有可用的容器名。";
            context.setPhase("prerequisite");
            context.setOutcome("error");
            context.setErrorMessage(message);
            context.setResult(message);
        }
        return next(command, context);
    }

    private boolean hasResolvedContainerName(String runId) {
        if (runId == null || runId.isBlank()) {
            return false;
        }
        return opsRunService.snapshot(runId)
                .map(session -> session.events().stream().anyMatch(this::isSuccessfulDescribeWithContainer))
                .orElse(false);
    }

    private boolean isSuccessfulDescribeWithContainer(OpsRunEvent event) {
        if (event == null || !"tool_result".equals(event.type())) {
            return false;
        }
        Map<String, Object> data = mapValue(event.data());
        if (!"catalog_ops".equals(stringValue(data.get("skill")))
                || !"catalog_describe_service".equals(stringValue(data.get("tool")))
                || !"success".equals(stringValue(data.get("outcome")))) {
            return false;
        }
        Map<String, Object> result = mapValue(data.get("result"));
        Map<String, Object> resultData = mapValue(result.get("data"));
        Map<String, Object> profile = mapValue(resultData.get("profile"));
        return !stringValue(profile.get("containerName")).isBlank();
    }

    private static Map<String, Object> mapValue(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, Object> out = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() != null) {
                out.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return out;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
