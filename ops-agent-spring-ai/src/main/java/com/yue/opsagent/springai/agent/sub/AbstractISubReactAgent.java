package com.yue.opsagent.springai.agent.sub;

import com.yue.opsagent.springai.config.OpsAiProperties;
import com.yue.opsagent.springai.observability.LlmCallTracer;
import com.yue.opsagent.springai.observability.OpsLogFormatter;
import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.registry.MasterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractISubReactAgent implements ISubAgent {

    private static final Logger log = LoggerFactory.getLogger(AbstractISubReactAgent.class);

    private final ChatClient chatClient;
    private final MasterRegistry masterRegistry;
    private final OpsSkillRegistry domainRegistry;
    private final int maxSubIters;
    private final LlmCallTracer llmCallTracer;

    protected AbstractISubReactAgent(
            ChatModel chatModel,
            MasterRegistry masterRegistry,
            OpsSkillRegistry domainRegistry,
            OpsAiProperties props,
            LlmCallTracer llmCallTracer) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.masterRegistry = masterRegistry;
        this.domainRegistry = domainRegistry;
        this.maxSubIters = props.getReact().getMaxSubIters();
        this.llmCallTracer = llmCallTracer;
    }

    @Override
    public String runReact(String task, Map<String, Object> context) {
        String taskPreview = task == null ? "" : task;
        if (taskPreview.length() > 160) {
            taskPreview = taskPreview.substring(0, 160) + "...";
        }
        log.info("[SubAgent] 进入 domain={} taskPreview={}", domainId(), taskPreview);
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(buildSystemPrompt()));
        messages.add(new UserMessage(buildFirstUserMessage(task, context)));

        for (int i = 0; i < maxSubIters; i++) {
            log.info("[SubAgent] domain={} 轮次 {}/{}", domainId(), i + 1, maxSubIters);
            String outbound = OpsLogFormatter.summarizeMessages(
                    messages, OpsLogFormatter.SUB_MSG_PART_MAX, OpsLogFormatter.LLM_BODY_MAX * 2);
            String raw = llmCallTracer.chatText(
                    "subagent:" + domainId() + "#" + i,
                    outbound,
                    () -> chatClient.prompt().messages(messages).call().chatResponse());
            var parsed = SubAgentJsonActions.parse(raw);
            if (parsed.isEmpty()) {
                log.warn("[SubAgent] domain={} 轮次 {} 模型输出无法解析为 JSON，已提示重试。snippet={}",
                        domainId(), i + 1, preview(raw, 120));
                messages.add(new AssistantMessage(raw));
                messages.add(new UserMessage(jsonFormatReminder()));
                continue;
            }
            if (parsed.get() instanceof SubAgentJsonActions.ParsedAction.FinalAction f) {
                log.info("[SubAgent] domain={} FINAL answerChars={}", domainId(), f.answer().length());
                return f.answer();
            }
            if (parsed.get() instanceof SubAgentJsonActions.ParsedAction.CallTool c) {
                if (!domainRegistry.toolNames().contains(c.tool())) {
                    log.warn("[SubAgent] domain={} 非法工具 tool={} 允许={}",
                            domainId(), c.tool(), domainRegistry.toolNames());
                    messages.add(new AssistantMessage(raw));
                    messages.add(new UserMessage(
                            "非法工具: " + c.tool() + "。仅允许: " + domainRegistry.toolNames()));
                    continue;
                }
                log.info("[SubAgent] domain={} CALL_TOOL tool={} argsKeys={}",
                        domainId(), c.tool(), c.args() == null ? List.of() : c.args().keySet());
                messages.add(new AssistantMessage(raw));
                Map<String, Object> args = c.args() == null ? Map.of() : c.args();
                ToolResult tr = masterRegistry.execute(domainRegistry.name(), c.tool(), args);
                if (tr instanceof ToolResult.Pending) {
                    log.warn("[SubAgent] domain={} 审批挂起 tool={}", domainId(), c.tool());
                    return "子Agent结束（审批挂起）: " + tr.toJson();
                }
                log.info("[SubAgent] domain={} 工具返回 status={} 结果截断↓\n{}",
                        domainId(),
                        tr.getClass().getSimpleName(),
                        OpsLogFormatter.truncate(tr.toJson(), OpsLogFormatter.LLM_BODY_MAX));
                messages.add(new UserMessage(
                        "观察（工具执行结果 JSON）：\n" + tr.toJson()
                                + "\n继续：输出下一 JSON 动作（CALL_TOOL 或 FINAL）。"));
            }
        }
        log.warn("[SubAgent] domain={} 达到最大轮次 {} 未收敛", domainId(), maxSubIters);
        return "子Agent达到最大轮次 (" + maxSubIters + ")，未收敛。";
    }

    private static String preview(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\n', ' ').trim();
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }

    private String buildSystemPrompt() {
        return """
                你是运维子Agent，仅处理一个技术域。你的目标是用最少工具调用回答调度者的明确问题。
                只允许输出**一段合法 JSON**（不要 markdown），格式二选一：
                1) {"action":"CALL_TOOL","tool":"<工具名>","args":{...键值...}}
                2) {"action":"FINAL","answer":"<给调度者的中文摘要>"}
                CALL_TOOL 时 args 必须与工具要求一致，不可为空对象（除非工具声明无参数）。
                工具返回已经能回答任务时必须 FINAL，不要继续扩展排查范围。
                如果工具返回空列表、空 result、not found、unknown、404、连接失败或指标为空，要把“查不到”作为事实返回，不要换一堆无关工具试探。
                如果任务是确认某服务是否存在，只需要给出：存在/不存在/无法确认 + 证据。

                可用工具（仅名称，细则在对话中按需下发）：
                """
                + domainRegistry.toolMenuBrief();
    }

    private String buildFirstUserMessage(String task, Map<String, Object> context) {
        return "任务：\n" + task + "\n\n上下文 JSON：\n" + context;
    }

    private static String jsonFormatReminder() {
        return "请只输出 JSON：{\"action\":\"CALL_TOOL\",\"tool\":\"...\",\"args\":{...}} 或 {\"action\":\"FINAL\",\"answer\":\"...\"}";
    }
}
