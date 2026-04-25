package com.yue.opsagent.springai.agent.sub;

import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.infrastructure.observability.LlmCallTracer;
import com.yue.opsagent.springai.skill.redis.RedisSkillRegistry;
import com.yue.opsagent.springai.skill.registry.MasterRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class RedisISubAgent extends AbstractISubReactAgent {

    public RedisISubAgent(
            ChatModel chatModel,
            MasterRegistry masterRegistry,
            RedisSkillRegistry registry,
            OpsAiProperties props,
            LlmCallTracer llmCallTracer) {
        super(chatModel, masterRegistry, registry, props, llmCallTracer);
    }

    @Override
    public String domainId() {
        return RedisSkillRegistry.SKILL_NAME;
    }

    @Override
    public String parentToolDescription() {
        return "委派 Redis 只读：INFO、慢日志、客户端、内存、GET/SCAN。传入 task 为自然语言任务说明。";
    }
}
