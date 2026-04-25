package com.yue.opsagent.springai.agent.sub;

import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.infrastructure.observability.LlmCallTracer;
import com.yue.opsagent.springai.skill.registry.MasterRegistry;
import com.yue.opsagent.springai.skill.rocketmq.RocketMqSkillRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class RocketMqISubAgent extends AbstractISubReactAgent {

    public RocketMqISubAgent(
            ChatModel chatModel,
            MasterRegistry masterRegistry,
            RocketMqSkillRegistry registry,
            OpsAiProperties props,
            LlmCallTracer llmCallTracer) {
        super(chatModel, masterRegistry, registry, props, llmCallTracer);
    }

    @Override
    public String domainId() {
        return RocketMqSkillRegistry.SKILL_NAME;
    }

    @Override
    public String parentToolDescription() {
        return "委派 RocketMQ：Topic 路由、消费统计、死信线索。传入 task 为自然语言任务说明。";
    }
}
