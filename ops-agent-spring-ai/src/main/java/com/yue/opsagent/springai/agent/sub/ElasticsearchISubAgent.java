package com.yue.opsagent.springai.agent.sub;

import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.infrastructure.observability.LlmCallTracer;
import com.yue.opsagent.springai.skill.elasticsearch.ElasticsearchSkillRegistry;
import com.yue.opsagent.springai.skill.registry.MasterRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchISubAgent extends AbstractISubReactAgent {

    public ElasticsearchISubAgent(
            ChatModel chatModel,
            MasterRegistry masterRegistry,
            ElasticsearchSkillRegistry registry,
            OpsAiProperties props,
            LlmCallTracer llmCallTracer) {
        super(chatModel, masterRegistry, registry, props, llmCallTracer);
    }

    @Override
    public String domainId() {
        return ElasticsearchSkillRegistry.SKILL_NAME;
    }

    @Override
    public String parentToolDescription() {
        return "委派 Elasticsearch：索引、搜索、计数、聚合（应用日志 nexus-* 与 SkyWalking sw_*；args 可带 cluster=skywalking）。传入 task 为自然语言任务说明。";
    }
}
