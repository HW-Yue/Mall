package com.yue.opsagent.springai.agent.parent;

import com.yue.opsagent.springai.agent.AgentContextHolder;
import com.yue.opsagent.springai.agent.registry.AgentToolRegistry;
import com.yue.opsagent.springai.agent.react.ReactAgentSpec;
import com.yue.opsagent.springai.agent.react.ReactRunner;
import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Outer ReAct for chat: tools delegate through {@link AgentToolRegistry}.
 */
@Component
public class ParentReactAgent {

    private static final Logger log = LoggerFactory.getLogger(ParentReactAgent.class);

    private final AgentToolRegistry agentToolRegistry;
    private final AgentContextHolder contextHolder;
    private final ReactRunner reactRunner;
    private final String parentSystemPrompt;
    private final int maxParentIters;

    public ParentReactAgent(
            AgentToolRegistry agentToolRegistry,
            AgentContextHolder contextHolder,
            ReactRunner reactRunner,
            OpsAiProperties props) {
        this.agentToolRegistry = agentToolRegistry;
        this.contextHolder = contextHolder;
        this.reactRunner = reactRunner;
        this.maxParentIters = props.getReact().getMaxParentIters();
        this.parentSystemPrompt = buildParentSystemPrompt(agentToolRegistry);
    }

    public String chat(String userMessage) {
        log.info("[Chat/ParentReact] 开始 userChars={}", userMessage == null ? 0 : userMessage.length());
        Map<String, Object> ctx = contextHolder.mutableCopyForSubAgent();
        String reply = reactRunner.run(new ReactAgentSpec(
                "ParentReactAgent",
                "parent-react",
                parentSystemPrompt,
                userMessage == null ? "" : userMessage,
                ctx,
                agentToolRegistry.reactTools(contextHolder::mutableCopyForSubAgent),
                maxParentIters));
        log.info("[Chat/ParentReact] 结束 replyChars={}（完整回复见上方 [LLM] 大模型回复）",
                reply == null ? 0 : reply.length());
        return reply;
    }

    private static String buildParentSystemPrompt(AgentToolRegistry agentToolRegistry) {
        return """
                你是运维调度 Agent。根据用户问题选择合适工具委派给子域；子域会执行多步工具调用。
                你必须只输出一段合法 JSON（不要 markdown），格式二选一：
                1) {"action":"CALL_TOOL","tool":"<子Agent工具名>","args":{"task":"<委派给子Agent的明确任务>"}}
                2) {"action":"FINAL","answer":"<给用户的中文结论>"}
                不要编造工具结果；必须通过工具完成需要查配置、查日志、查指标的操作。
                CALL_TOOL 的 args 必须包含 task 字段。
                可用委派工具（名称即函数名）：
                """
                + agentToolRegistry.buildMenu();
    }
}
