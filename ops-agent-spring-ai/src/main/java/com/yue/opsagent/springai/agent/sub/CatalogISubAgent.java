package com.yue.opsagent.springai.agent.sub;

import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.infrastructure.observability.LlmCallTracer;
import com.yue.opsagent.springai.infrastructure.observability.OpsAiMetrics;
import com.yue.opsagent.springai.skill.catalog.CatalogSkillRegistry;
import com.yue.opsagent.springai.skill.registry.MasterRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class CatalogISubAgent extends AbstractISubReactAgent {

    public CatalogISubAgent(
            ChatModel chatModel,
            MasterRegistry masterRegistry,
            CatalogSkillRegistry registry,
            OpsAiProperties props,
            LlmCallTracer llmCallTracer,
            OpsAiMetrics metrics) {
        super(chatModel, masterRegistry, registry, props, llmCallTracer, metrics);
    }

    @Override
    public String domainId() {
        return CatalogSkillRegistry.SKILL_NAME;
    }

    @Override
    public String parentToolDescription() {
        return "Catalog Skill：可先列出当前服务名或 Topic，再逐个查询候选服务的 application/compose service/container/configEntries 以及 topic/table/pool 静态归属。";
    }
}
