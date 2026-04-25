package com.yue.opsagent.springai.agent.parent;

import com.yue.opsagent.springai.agent.OrchestrationContextHolder;
import com.yue.opsagent.springai.agent.sub.ISubAgent;
import com.yue.opsagent.springai.observability.LlmCallTracer;
import com.yue.opsagent.springai.observability.OpsLogFormatter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Outer ReAct for chat: tools delegate to {@link ISubAgent#runReact}.
 */
@Component
public class ParentReactAgent {

    private static final Logger log = LoggerFactory.getLogger(ParentReactAgent.class);

    private final ChatClient chatClient;
    private final String parentSystemPrompt;
    private final LlmCallTracer llmCallTracer;

    public ParentReactAgent(
            ChatModel chatModel,
            List<ISubAgent> agents,
            OrchestrationContextHolder contextHolder,
            LlmCallTracer llmCallTracer) {
        this.llmCallTracer = llmCallTracer;
        List<ToolCallback> callbacks = new ArrayList<>();
        for (ISubAgent a : agents) {
            callbacks.add(FunctionToolCallback.builder(
                            a.parentToolName(),
                            (DelegateTask t) -> a.runReact(
                                    t.task() == null ? "" : t.task(),
                                    contextHolder.mutableCopyForSubAgent()))
                    .description(a.parentToolDescription())
                    .inputType(DelegateTask.class)
                    .build());
        }
        this.chatClient = ChatClient.builder(chatModel)
                .defaultToolCallbacks(callbacks)
                .build();
        this.parentSystemPrompt = buildParentSystemPrompt(agents);
    }

    public String chat(String userMessage) {
        log.info("[Chat/ParentReact] 开始 userChars={}", userMessage == null ? 0 : userMessage.length());
        String outbound = "[system]\n"
                + OpsLogFormatter.truncate(parentSystemPrompt, OpsLogFormatter.LLM_BODY_MAX) + "\n[user]\n"
                + OpsLogFormatter.truncate(userMessage == null ? "" : userMessage, OpsLogFormatter.LLM_BODY_MAX);
        String reply = llmCallTracer.chatText(
                "parent-react",
                outbound,
                () -> chatClient.prompt()
                        .system(parentSystemPrompt)
                        .user(userMessage)
                        .call()
                        .chatResponse());
        log.info("[Chat/ParentReact] 结束 replyChars={}（完整回复见上方 [LLM] 大模型回复）",
                reply == null ? 0 : reply.length());
        return reply;
    }

    private static String buildParentSystemPrompt(List<ISubAgent> agents) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                你是运维调度 Agent。根据用户问题选择合适工具委派给子域；子域会执行多步工具调用。
                不要编造工具结果；必须通过工具完成需要查配置、查日志、查指标的操作。
                可用委派工具（名称即函数名）：
                """);
        for (ISubAgent a : agents) {
            sb.append("- ")
                    .append(a.parentToolName())
                    .append(": ")
                    .append(a.parentToolDescription())
                    .append('\n');
        }
        return sb.toString();
    }
}
