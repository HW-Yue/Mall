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
        return "Elasticsearch Skill：索引、按服务检索错误日志、搜索、计数、聚合（应用日志 nexus-* 与 SkyWalking sw_*；args 可带 cluster=skywalking）。日志排查默认优先按服务查错误样本，不要先手写 DSL。";
    }
}
