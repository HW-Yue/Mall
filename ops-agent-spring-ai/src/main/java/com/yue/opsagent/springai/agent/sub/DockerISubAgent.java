package com.yue.opsagent.springai.agent.sub;

import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.infrastructure.observability.LlmCallTracer;
import com.yue.opsagent.springai.skill.docker.DockerSkillRegistry;
import com.yue.opsagent.springai.skill.registry.MasterRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class DockerISubAgent extends AbstractISubReactAgent {

    public DockerISubAgent(
            ChatModel chatModel,
            MasterRegistry masterRegistry,
            DockerSkillRegistry registry,
            OpsAiProperties props,
            LlmCallTracer llmCallTracer) {
        super(chatModel, masterRegistry, registry, props, llmCallTracer);
    }

    @Override
    public String domainId() {
        return DockerSkillRegistry.SKILL_NAME;
    }

    @Override
    public String parentToolDescription() {
        return "Docker Skill：日志、stats、inspect、受控 exec。传入 task 为自然语言任务说明。";
    }
}
