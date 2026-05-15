package com.yue.opsagent.springai.agent.sub;

import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.infrastructure.observability.LlmCallTracer;
import com.yue.opsagent.springai.skill.nacos.NacosSkillRegistry;
import com.yue.opsagent.springai.skill.registry.MasterRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class NacosISubAgent extends AbstractISubReactAgent {

    public NacosISubAgent(
            ChatModel chatModel,
            MasterRegistry masterRegistry,
            NacosSkillRegistry nacosSkillRegistry,
            OpsAiProperties props,
            LlmCallTracer llmCallTracer) {
        super(chatModel, masterRegistry, nacosSkillRegistry, props, llmCallTracer);
    }

    @Override
    public String domainId() {
        return NacosSkillRegistry.SKILL_NAME;
    }

    @Override
    public String parentToolDescription() {
        return "Nacos Skill：配置读/写（写可能审批）、服务实例与服务列表。只有拿到 Catalog 返回的明确 dataId/group 后才允许读配置；传入 task 为自然语言任务说明。";
    }
}
