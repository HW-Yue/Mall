package com.yue.opsagent.springai.agent.sub;

import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.infrastructure.observability.LlmCallTracer;
import com.yue.opsagent.springai.infrastructure.observability.OpsAiMetrics;
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
            LlmCallTracer llmCallTracer,
            OpsAiMetrics metrics) {
        super(chatModel, masterRegistry, registry, props, llmCallTracer, metrics);
    }

    @Override
    public String domainId() {
        return DockerSkillRegistry.SKILL_NAME;
    }

    @Override
    public String parentToolDescription() {
        return "Docker Skill：日志、stats、inspect、受控 exec。进入本子域前必须先从 Catalog 拿到非空 containerName，不能凭空猜容器名。";
    }
}
