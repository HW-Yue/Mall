package com.yue.opsagent.springai.skill.registry.rule;

import com.yue.opsagent.springai.domain.opsroute.OpsRunEvent;
import com.yue.opsagent.springai.service.OpsRunService;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.registry.ToolExecutionCommand;
import com.yue.opsagent.springai.skill.registry.ToolExecutionContext;
import com.yue.opsagent.springai.skill.registry.ToolExecutionRuleFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Order(25)
public class NacosConfigPrerequisiteRuleFilter implements ToolExecutionRuleFilter {

    private final OpsRunService opsRunService;

    public NacosConfigPrerequisiteRuleFilter(OpsRunService opsRunService) {
        this.opsRunService = opsRunService;
    }

    @Override
    public ToolResult apply(ToolExecutionCommand command, ToolExecutionContext context) {
        if (context.hasResult()) {
            return next(command, context);
        }
        if (!"nacos_config".equals(command.skillName()) || !"nacos_get_config".equals(command.toolName())) {
            return next(command, context);
        }
        String runId = context.getRunId();
        if (runId == null || runId.isBlank()) {
            return next(command, context);
        }
        String dataId = stringValue(command.args().get("dataId"));
        if (dataId.isBlank()) {
            return next(command, context);
        }
        String group = stringValue(command.args().get("group"));
        String normalizedGroup = group.isBlank() ? "DEFAULT_GROUP" : group;
        ResolvedConfigFacts facts = resolvedConfigFacts(runId);
        if (facts.entries().isEmpty()) {
            return reject(command, context, "nacos_get_config 前必须先通过 catalog_describe_service 获取可用 dataId/group；当前 run 里还没有解析到任何配置入口。");
        }
        Set<String> groups = facts.groupsByDataId().get(dataId);
        if (groups == null || groups.isEmpty()) {
            return reject(command, context, "nacos_get_config 前必须使用 catalog_describe_service 返回的 dataId/group；当前 dataId="
                    + dataId + " 不在本轮已解析的配置入口里。");
        }
        if (!groups.contains(normalizedGroup)) {
            return reject(command, context, "nacos_get_config 前必须使用 catalog_describe_service 返回的 dataId/group；当前 run 已解析到 dataId="
                    + dataId + " 的可用 group=" + groups + "，而不是 " + normalizedGroup + "。");
        }
        return next(command, context);
    }

    private ToolResult reject(ToolExecutionCommand command, ToolExecutionContext context, String message) {
        context.setPhase("prerequisite");
        context.setOutcome("error");
        context.setErrorMessage(message);
        context.setResult(ToolResult.error(message));
        return next(command, context);
    }

    private ResolvedConfigFacts resolvedConfigFacts(String runId) {
        Map<String, Set<String>> groupsByDataId = new LinkedHashMap<>();
        List<Map<String, String>> entries = opsRunService.snapshot(runId)
                .map(session -> session.events().stream()
                        .filter(this::isSuccessfulDescribe)
                        .flatMap(event -> configEntries(event).stream())
                        .toList())
                .orElse(List.of());
        for (Map<String, String> entry : entries) {
            String dataId = stringValue(entry.get("dataId"));
            String group = stringValue(entry.get("group"));
            if (dataId.isBlank()) {
                continue;
            }
            groupsByDataId.computeIfAbsent(dataId, ignored -> new LinkedHashSet<>())
                    .add(group.isBlank() ? "DEFAULT_GROUP" : group);
        }
        return new ResolvedConfigFacts(entries, groupsByDataId);
    }

    private boolean isSuccessfulDescribe(OpsRunEvent event) {
        if (event == null || !"tool_result".equals(event.type())) {
            return false;
        }
        Map<String, Object> data = mapValue(event.data());
        return "catalog_ops".equals(stringValue(data.get("skill")))
                && "catalog_describe_service".equals(stringValue(data.get("tool")))
                && "success".equals(stringValue(data.get("outcome")));
    }

    private List<Map<String, String>> configEntries(OpsRunEvent event) {
        Map<String, Object> data = mapValue(event.data());
        Map<String, Object> result = mapValue(data.get("result"));
        Map<String, Object> resultData = mapValue(result.get("data"));
        Map<String, Object> profile = mapValue(resultData.get("profile"));
        return listValue(profile.get("configEntries")).stream()
                .map(this::configEntry)
                .filter(entry -> !entry.get("dataId").isBlank())
                .toList();
    }

    private Map<String, String> configEntry(Object value) {
        Map<String, Object> raw = mapValue(value);
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("dataId", stringValue(raw.get("dataId")));
        entry.put("group", stringValue(raw.get("group")));
        return entry;
    }

    private static Map<String, Object> mapValue(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() != null) {
                out.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return out;
    }

    private static List<Object> listValue(Object value) {
        if (value instanceof List<?> list) {
            return List.copyOf(list);
        }
        return List.of();
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private record ResolvedConfigFacts(
            List<Map<String, String>> entries,
            Map<String, Set<String>> groupsByDataId
    ) {
    }
}
