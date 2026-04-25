package com.yue.opsagent.springai.skill.api;

import java.util.Map;
import java.util.Set;

/**
 * One domain skill: menu line, detailed prompt fragment, and tool routing.
 */
public interface OpsSkillRegistry {

    /** Stable id, e.g. {@code elasticsearch_ops}. */
    String name();

    String description();

    String promptFragment();

    Set<String> toolNames();

    ToolResult execute(String toolName, Map<String, Object> args);

    /** Tools that must pass human approval before side effects. */
    default boolean requiresApproval(String toolName) {
        return false;
    }

    /** Short menu for sub-agent round 1 (tool ids + one-line hint). */
    default String toolMenuBrief() {
        return promptFragment();
    }

    /** Parameter help for a single tool (sub-agent progressive disclosure). */
    default String toolSpecification(String toolName) {
        if (!toolNames().contains(toolName)) {
            return "未知工具: " + toolName + "。允许: " + toolNames();
        }
        return "工具 " + toolName + " 的说明见下：\n" + promptFragment();
    }
}
