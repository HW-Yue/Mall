package com.yue.opsagent.springai.agent.sub;

import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.infrastructure.observability.LlmCallTracer;
import com.yue.opsagent.springai.skill.mysql.MysqlSkillRegistry;
import com.yue.opsagent.springai.skill.registry.MasterRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class MysqlISubAgent extends AbstractISubReactAgent {

    public MysqlISubAgent(
            ChatModel chatModel,
            MasterRegistry masterRegistry,
            MysqlSkillRegistry registry,
            OpsAiProperties props,
            LlmCallTracer llmCallTracer) {
        super(chatModel, masterRegistry, registry, props, llmCallTracer);
    }

    @Override
    public String domainId() {
        return MysqlSkillRegistry.SKILL_NAME;
    }

    @Override
    public String parentToolDescription() {
        return "委派 MySQL 只读诊断：会话、状态、锁、慢查询。传入 task 为自然语言任务说明。";
    }
}
