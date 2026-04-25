package com.yue.opsagent.springai.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring AI 对话映射为 OpenTelemetry GenAI Span，经 OTLP 上报 SkyWalking OAP 10.4 Virtual GenAI。
 *
 * <p>识别规则：span 含 {@code gen_ai.response.model} 等语义属性。</p>
 */
@Component
@ConditionalOnBean(Tracer.class)
public class GenAiTelemetry {

    private static final String ATTR_PROVIDER = "gen_ai.provider.name";
    private static final String ATTR_MODEL = "gen_ai.response.model";
    private static final String ATTR_INPUT_TOKENS = "gen_ai.usage.input_tokens";
    private static final String ATTR_OUTPUT_TOKENS = "gen_ai.usage.output_tokens";
    private static final String ATTR_TOTAL_TOKENS = "gen_ai.usage.total_tokens";
    private static final String ATTR_AGENT_NAME = "spring.ai.agent.logical_name";
    private static final String ATTR_OPERATION = "gen_ai.operation.name";

    private final Tracer tracer;
    private final Map<String, SpanHolder> activeSpans = new ConcurrentHashMap<>();

    public GenAiTelemetry(Tracer tracer) {
        this.tracer = tracer;
    }

    public void startReasoning(String logicalAgentName, String modelNameHint) {
        String key = spanKey(logicalAgentName);
        Span span = tracer.spanBuilder("genai.chat")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(ATTR_PROVIDER, "dashscope")
                .setAttribute(ATTR_MODEL, modelNameHint != null ? modelNameHint : "unknown")
                .setAttribute(ATTR_AGENT_NAME, logicalAgentName)
                .setAttribute(ATTR_OPERATION, "chat.completion")
                .startSpan();
        Scope scope = span.makeCurrent();
        activeSpans.put(key, new SpanHolder(span, scope));
    }

    public void endReasoning(String logicalAgentName, String modelName, Usage usage) {
        String key = spanKey(logicalAgentName);
        SpanHolder holder = activeSpans.remove(key);
        if (holder == null) {
            return;
        }
        try {
            if (modelName != null) {
                holder.span.setAttribute(ATTR_MODEL, modelName);
            }
            if (usage != null) {
                if (usage.getPromptTokens() != null) {
                    holder.span.setAttribute(ATTR_INPUT_TOKENS, usage.getPromptTokens().longValue());
                }
                if (usage.getCompletionTokens() != null) {
                    holder.span.setAttribute(ATTR_OUTPUT_TOKENS, usage.getCompletionTokens().longValue());
                }
                if (usage.getTotalTokens() != null) {
                    holder.span.setAttribute(ATTR_TOTAL_TOKENS, usage.getTotalTokens().longValue());
                }
            }
            holder.span.setStatus(StatusCode.OK);
        } finally {
            holder.close();
        }
    }

    public void endWithError(String logicalAgentName, Throwable error) {
        String key = spanKey(logicalAgentName);
        SpanHolder holder = activeSpans.remove(key);
        if (holder == null) {
            return;
        }
        try {
            holder.span.recordException(error);
            holder.span.setStatus(StatusCode.ERROR,
                    error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName());
        } finally {
            holder.close();
        }
    }

    public void ensureEnded(String logicalAgentName) {
        String key = spanKey(logicalAgentName);
        SpanHolder holder = activeSpans.remove(key);
        if (holder != null) {
            holder.close();
        }
    }

    private static String spanKey(String agentName) {
        return agentName + "@" + Thread.currentThread().threadId();
    }

    private static final class SpanHolder {
        final Span span;
        final Scope scope;

        SpanHolder(Span span, Scope scope) {
            this.span = span;
            this.scope = scope;
        }

        void close() {
            try {
                scope.close();
            } finally {
                span.end();
            }
        }
    }
}
