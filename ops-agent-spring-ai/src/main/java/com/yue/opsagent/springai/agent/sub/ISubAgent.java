package com.yue.opsagent.springai.agent.sub;

import java.util.Map;

public interface ISubAgent {

    /** Same id as {@link com.yue.opsagent.springai.skill.api.OpsSkillRegistry#name()}. */
    String domainId();

    /** Shown in parent system prompt / tool description. */
    String parentToolDescription();

    /** Spring AI tool name（与 {@link #domainId()} 一致，仅将连字符替换为下划线）。 */
    default String parentToolName() {
        return domainId().replace('-', '_');
    }

    /**
     * Inner ReAct loop for this domain; returns summary for parent / SOP orchestrator.
     */
    String runReact(String task, Map<String, Object> context);
}
