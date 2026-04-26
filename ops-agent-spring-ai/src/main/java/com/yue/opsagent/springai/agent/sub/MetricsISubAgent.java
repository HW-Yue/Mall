package com.yue.opsagent.springai.agent.sub;

import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.infrastructure.observability.LlmCallTracer;
import com.yue.opsagent.springai.skill.prometheus.MetricsSkillRegistry;
import com.yue.opsagent.springai.skill.registry.MasterRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class MetricsISubAgent extends AbstractISubReactAgent {

    public MetricsISubAgent(
            ChatModel chatModel,
            MasterRegistry masterRegistry,
            MetricsSkillRegistry registry,
            OpsAiProperties props,
            LlmCallTracer llmCallTracer) {
        super(chatModel, masterRegistry, registry, props, llmCallTracer);
    }

    @Override
    public String domainId() {
        return MetricsSkillRegistry.SKILL_NAME;
    }

    @Override
    public String parentToolDescription() {
        return "Prometheus Skill：Sentinel/DynamicTP/JVM/业务 PromQL。传入 task 为自然语言任务说明。";
    }
}
