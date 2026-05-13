package com.yue.opsagent.springai.skill.api;

import java.util.Map;
import java.util.Set;

/**
 * One domain skill: menu line, detailed prompt fragment, and tool routing.
 */
public interface OpsSkillRegistry {

    String DEFAULT_HELP_TOOL_NAME = "skill_tool_help";

    /** JSON args key for {@link #helpToolName()} (value = data tool id). Must match {@code SkillToolHelp.ARG_TOOL}. */
    String HELP_ARG_TOOL = "tool";

    /** Stable id, e.g. {@code elasticsearch_ops}. */
    String name();

    String description();

    String promptFragment();

    Set<String> toolNames();

    ToolResult execute(String toolName, Map<String, Object> args);

    /**
     * Full documentation for a data tool (excludes {@link #helpToolName()}). Return blank if unknown.
     */
    String documentationForDataTool(String dataToolName);

    /** Tools that must pass human approval before side effects. */
    default boolean requiresApproval(String toolName) {
        return false;
    }

    default String helpToolName() {
        return DEFAULT_HELP_TOOL_NAME;
    }

    default String helpToolMenuLine() {
        return "- "
                + helpToolName()
                + ": 查询某一业务工具的完整参数、约束与注意事项；CALL_TOOL 时 args 必须含字符串字段 "
                + HELP_ARG_TOOL
                + "，值为下方列表中的业务工具名（勿将本工具自身作为查询目标）。";
    }

    default String helpToolSpecification() {
        return helpToolName()
                + ": args 必填键 "
                + HELP_ARG_TOOL
                + "(string)，值为除 "
                + helpToolName()
                + " 外的工具名；返回该工具完整说明。";
    }

    /** Short menu for sub-agent round 1 (tool ids + one-line hint). */
    default String toolMenuBrief() {
        return promptFragment();
    }

    /** Parameter help for a single tool (sub-agent progressive disclosure). */
    default String toolSpecification(String toolName) {
        if (helpToolName().equals(toolName)) {
            return helpToolSpecification();
        }
        if (!toolNames().contains(toolName)) {
            return "未知工具: " + toolName + "。允许: " + toolNames();
        }
        String doc = documentationForDataTool(toolName);
        if (doc != null && !doc.isBlank()) {
            return doc;
        }
        return "工具 "
                + toolName
                + "：请调用 "
                + helpToolName()
                + "，args 为 {\\\""
                + HELP_ARG_TOOL
                + "\\\":\\\""
                + toolName
                + "\\\"} 获取完整说明。";
    }
}
