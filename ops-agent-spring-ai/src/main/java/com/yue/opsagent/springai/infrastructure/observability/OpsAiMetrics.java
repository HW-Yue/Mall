package com.yue.opsagent.springai.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * GenAI / ReAct 指标采集（Micrometer）。
 *
 * <p>所有指标经 {@code micrometer-registry-prometheus} 暴露在 {@code /actuator/prometheus}，
 * Prometheus 已通过 Nacos 服务发现自动抓取本应用，无需额外抓取配置。</p>
 *
 * <p>暴露后的 Prometheus 指标名（点号转下划线）：</p>
 * <ul>
 *   <li>{@code llm_call_duration_seconds}（含 {@code _bucket}，支持 histogram_quantile 出 P50/P95/P99）</li>
 *   <li>{@code llm_tokens_total{type="input|output"}}</li>
 *   <li>{@code llm_call_errors_total}</li>
 *   <li>{@code react_iterations}（含 {@code _bucket}，收敛轮次分布）</li>
 *   <li>{@code react_parse_retry_total} / {@code react_unconverged_total} / {@code react_illegal_tool_total}</li>
 * </ul>
 *
 * <p>标签均为有界低基数：{@code agent}（逻辑名）、{@code model}、{@code type}。</p>
 */
@Component
public class OpsAiMetrics {

    private final MeterRegistry registry;

    public OpsAiMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** 记录一次 LLM 调用耗时。 */
    public void recordLlmCall(String agent, String model, long durationNanos) {
        Timer.builder("llm.call.duration")
                .description("LLM chat completion latency")
                .tag("agent", agentTag(agent))
                .tag("model", safe(model))
                .minimumExpectedValue(Duration.ofMillis(50))
                .maximumExpectedValue(Duration.ofSeconds(60))
                .publishPercentileHistogram()
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    /** 记录一次 LLM 调用的 token 用量（input / output 分开）。 */
    public void recordTokens(String agent, Integer promptTokens, Integer completionTokens) {
        if (promptTokens != null && promptTokens > 0) {
            tokenCounter(agent, "input").increment(promptTokens);
        }
        if (completionTokens != null && completionTokens > 0) {
            tokenCounter(agent, "output").increment(completionTokens);
        }
    }

    /** 记录一次 LLM 调用失败。 */
    public void recordLlmError(String agent) {
        Counter.builder("llm.call.errors")
                .description("LLM chat completion errors")
                .tag("agent", agentTag(agent))
                .register(registry)
                .increment();
    }

    /** 记录一次 ReAct 循环最终收敛/结束时的轮次。 */
    public void recordReactIterations(String agent, int iterations) {
        DistributionSummary.builder("react.iterations")
                .description("ReAct loop iterations until termination")
                .tag("agent", safe(agent))
                .minimumExpectedValue(1.0)
                .maximumExpectedValue(64.0)
                .publishPercentileHistogram()
                .register(registry)
                .record(iterations);
    }

    /** JSON 动作解析失败、提示模型重试。 */
    public void incReactParseRetry(String agent) {
        reactCounter("react.parse.retry", "ReAct JSON action parse failures", agent).increment();
    }

    /** 达到最大轮次仍未收敛。 */
    public void incReactUnconverged(String agent) {
        reactCounter("react.unconverged", "ReAct runs hitting max iterations without FINAL", agent).increment();
    }

    /** 模型请求了不存在的工具。 */
    public void incReactIllegalTool(String agent) {
        reactCounter("react.illegal.tool", "ReAct illegal tool requests", agent).increment();
    }

    private Counter tokenCounter(String agent, String type) {
        return Counter.builder("llm.tokens")
                .description("LLM token usage")
                .tag("agent", agentTag(agent))
                .tag("type", type)
                .register(registry);
    }

    private Counter reactCounter(String name, String description, String agent) {
        return Counter.builder(name)
                .description(description)
                .tag("agent", safe(agent))
                .register(registry);
    }

    private static String safe(String v) {
        return (v == null || v.isBlank()) ? "unknown" : v;
    }

    /**
     * 规范化 agent 标签：剥离 ReAct trace 命名里的 {@code #迭代序号} 后缀，
     * 避免把同一逻辑 agent 的指标按轮次拆成多条高基数序列。
     */
    private static String agentTag(String agent) {
        if (agent == null || agent.isBlank()) {
            return "unknown";
        }
        int hash = agent.indexOf('#');
        return hash > 0 ? agent.substring(0, hash) : agent;
    }
}
