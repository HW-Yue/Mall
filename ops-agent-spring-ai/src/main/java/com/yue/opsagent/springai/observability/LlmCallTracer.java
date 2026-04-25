package com.yue.opsagent.springai.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 包装 {@link ChatClient} 同步调用：在启用 OTLP 时为每次 LLM 往返创建 GenAI span；
 * INFO 日志输出「发往模型 / 模型回复」截断正文，便于 ELK 排查与大模型交互过程。
 */
@Component
public class LlmCallTracer {

    private static final Logger log = LoggerFactory.getLogger(LlmCallTracer.class);

    private final GenAiTelemetry telemetry;
    private final String defaultModelName;

    public LlmCallTracer(
            ObjectProvider<GenAiTelemetry> telemetryProvider,
            @Value("${spring.ai.dashscope.chat.options.model:qwen-max}") String defaultModelName) {
        this.telemetry = telemetryProvider.getIfAvailable();
        this.defaultModelName = defaultModelName;
    }

    /**
     * @param logicalAgentKey 逻辑名（如 parent-react、subagent:docker_ops），用于 SkyWalking 上区分调用栈
     */
    public String chatText(String logicalAgentKey, Supplier<ChatResponse> call) {
        return chatText(logicalAgentKey, null, call);
    }

    /**
     * @param outboundSummary 本次请求发往模型的内容摘要（system/user/多轮等），为 null 时仅打开始/结束行与字数
     */
    public String chatText(String logicalAgentKey, String outboundSummary, Supplier<ChatResponse> call) {
        if (telemetry == null) {
            logOutbound(logicalAgentKey, outboundSummary);
            try {
                ChatResponse r = call.get();
                return logAndExtractText(logicalAgentKey, r, r.getMetadata());
            } catch (RuntimeException e) {
                log.warn("[LLM] 调用失败 key={} err={}", logicalAgentKey, e.toString());
                throw e;
            }
        }
        telemetry.startReasoning(logicalAgentKey, defaultModelName);
        logOutbound(logicalAgentKey, outboundSummary);
        try {
            ChatResponse r = call.get();
            var meta = r.getMetadata();
            String model = meta != null && meta.getModel() != null ? meta.getModel() : defaultModelName;
            telemetry.endReasoning(logicalAgentKey, model, meta != null ? meta.getUsage() : null);
            return logAndExtractText(logicalAgentKey, r, meta);
        } catch (RuntimeException e) {
            telemetry.endWithError(logicalAgentKey, e);
            log.warn("[LLM] 调用失败 key={} err={}", logicalAgentKey, e.toString());
            throw e;
        }
    }

    private void logOutbound(String logicalAgentKey, String outboundSummary) {
        if (outboundSummary != null && !outboundSummary.isBlank()) {
            log.info("[LLM] key={} model={} 发往大模型 ↓\n{}", logicalAgentKey, defaultModelName,
                    OpsLogFormatter.truncate(outboundSummary.trim(), OpsLogFormatter.LLM_BODY_MAX));
        } else {
            log.info("[LLM] 调用开始 key={} model={}（未记录请求正文）", logicalAgentKey, defaultModelName);
        }
    }

    private String logAndExtractText(String logicalAgentKey, ChatResponse r, ChatResponseMetadata meta) {
        String t = r.getResult().getOutput().getText();
        Integer pt = null;
        Integer ct = null;
        if (meta != null && meta.getUsage() != null) {
            var u = meta.getUsage();
            if (u.getPromptTokens() != null) {
                pt = u.getPromptTokens();
            }
            if (u.getCompletionTokens() != null) {
                ct = u.getCompletionTokens();
            }
        }
        log.info("[LLM] key={} 大模型回复 replyChars={} promptTokens={} completionTokens={} ↓\n{}",
                logicalAgentKey,
                t == null ? 0 : t.length(),
                pt,
                ct,
                OpsLogFormatter.truncate(t == null ? "" : t, OpsLogFormatter.LLM_BODY_MAX));
        return t;
    }
}
