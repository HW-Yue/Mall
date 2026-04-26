package com.yue.opsagent.springai.agent.react;

import com.yue.opsagent.springai.infrastructure.observability.LlmCallTracer;
import com.yue.opsagent.springai.infrastructure.observability.OpsLogFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReactRunner {

    private static final Logger log = LoggerFactory.getLogger(ReactRunner.class);

    private final ChatClient chatClient;
    private final LlmCallTracer llmCallTracer;

    public ReactRunner(ChatModel chatModel, LlmCallTracer llmCallTracer) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.llmCallTracer = llmCallTracer;
    }

    public String run(ReactAgentSpec spec) {
        Map<String, ReactTool> tools = indexTools(spec.tools());
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(spec.systemPrompt()));
        messages.add(new UserMessage(spec.userMessage()));

        for (int i = 0; i < spec.maxIters(); i++) {
            log.info("[ReactRunner] agent={} 轮次 {}/{}", spec.agentName(), i + 1, spec.maxIters());
            String outbound = OpsLogFormatter.summarizeMessages(
                    messages, OpsLogFormatter.SUB_MSG_PART_MAX, OpsLogFormatter.LLM_BODY_MAX * 2);
            String raw = llmCallTracer.chatText(
                    spec.traceName() + "#" + i,
                    outbound,
                    () -> chatClient.prompt().messages(messages).call().chatResponse());
            var parsed = ReactActionParser.parse(raw);
            if (parsed.isEmpty()) {
                log.warn("[ReactRunner] agent={} 轮次 {} 模型输出无法解析为 JSON，已提示重试。snippet={}",
                        spec.agentName(), i + 1, preview(raw, 120));
                messages.add(new AssistantMessage(raw == null ? "" : raw));
                messages.add(new UserMessage(ReactActionParser.jsonFormatReminder()));
                continue;
            }
            if (parsed.get() instanceof ReactActionParser.ParsedAction.FinalAction f) {
                log.info("[ReactRunner] agent={} FINAL answerChars={}", spec.agentName(), f.answer().length());
                return f.answer();
            }
            if (parsed.get() instanceof ReactActionParser.ParsedAction.CallTool c) {
                ReactTool tool = tools.get(c.tool());
                if (tool == null) {
                    log.warn("[ReactRunner] agent={} 非法工具 tool={} 允许={}",
                            spec.agentName(), c.tool(), tools.keySet());
                    messages.add(new AssistantMessage(raw));
                    messages.add(new UserMessage("非法工具: " + c.tool() + "。仅允许: " + tools.keySet()));
                    continue;
                }
                log.info("[ReactRunner] agent={} CALL_TOOL tool={} argsKeys={}",
                        spec.agentName(), c.tool(), c.args() == null ? List.of() : c.args().keySet());
                messages.add(new AssistantMessage(raw));
                Map<String, Object> args = c.args() == null ? Map.of() : c.args();
                String observation = tool.execute(args, spec.context() == null ? Map.of() : spec.context());
                log.info("[ReactRunner] agent={} tool={} observation截断↓\n{}",
                        spec.agentName(),
                        c.tool(),
                        OpsLogFormatter.truncate(observation, OpsLogFormatter.LLM_BODY_MAX));
                messages.add(new UserMessage(
                        "观察（工具执行结果）：\n" + observation
                                + "\n继续：输出下一 JSON 动作（CALL_TOOL 或 FINAL）。"));
            }
        }
        log.warn("[ReactRunner] agent={} 达到最大轮次 {} 未收敛", spec.agentName(), spec.maxIters());
        return spec.agentName() + "达到最大轮次 (" + spec.maxIters() + ")，未收敛。";
    }

    private static Map<String, ReactTool> indexTools(List<ReactTool> tools) {
        Map<String, ReactTool> index = new LinkedHashMap<>();
        if (tools == null) {
            return index;
        }
        for (ReactTool tool : tools) {
            index.put(tool.name(), tool);
        }
        return index;
    }

    private static String preview(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\n', ' ').trim();
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }
}
