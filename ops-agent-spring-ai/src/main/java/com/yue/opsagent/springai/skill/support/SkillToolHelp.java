package com.yue.opsagent.springai.skill.support;

import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared handler for the per-skill help tool ({@link OpsSkillRegistry#helpToolName()}).
 */
public final class SkillToolHelp {

    /** Must match {@link com.yue.opsagent.springai.skill.api.OpsSkillRegistry#HELP_ARG_TOOL}. */
    public static final String ARG_TOOL = OpsSkillRegistry.HELP_ARG_TOOL;

    private SkillToolHelp() {
    }

    /**
     * @return non-null {@link ToolResult} when {@code invokedTool} is the help tool; otherwise {@code null}
     */
    public static ToolResult tryExecute(OpsSkillRegistry reg, String invokedTool, Map<String, Object> args) {
        if (!reg.helpToolName().equals(invokedTool)) {
            return null;
        }
        Map<String, Object> a = args == null ? Map.of() : args;
        String target = argString(a, ARG_TOOL);
        if (target.isBlank()) {
            return ToolResult.error(reg.helpToolName() + ": args 必须包含非空字段 \"" + ARG_TOOL + "\"（业务工具名）");
        }
        Set<String> dataTools = dataToolNames(reg);
        if (!dataTools.contains(target)) {
            return ToolResult.error(
                    "unknown tool for " + reg.helpToolName() + ": " + target + "。允许的业务工具: " + dataTools);
        }
        String doc = reg.documentationForDataTool(target);
        if (doc == null || doc.isBlank()) {
            return ToolResult.error(reg.helpToolName() + ": 无文档: " + target);
        }
        return ToolResult.ok("usage", Map.of("tool", target, "usage", doc));
    }

    public static Set<String> toolNamesWithHelp(Set<String> dataToolNames, OpsSkillRegistry reg) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(dataToolNames);
        merged.add(reg.helpToolName());
        return Set.copyOf(merged);
    }

    private static Set<String> dataToolNames(OpsSkillRegistry reg) {
        return reg.toolNames().stream()
                .filter(n -> !reg.helpToolName().equals(n))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String argString(Map<String, Object> a, String key) {
        Object v = a.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }
}
