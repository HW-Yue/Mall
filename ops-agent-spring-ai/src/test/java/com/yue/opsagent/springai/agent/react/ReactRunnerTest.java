package com.yue.opsagent.springai.agent.react;

import com.yue.opsagent.springai.infrastructure.observability.LlmCallTracer;
import com.yue.opsagent.springai.infrastructure.observability.OpsAiMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReactRunnerTest {

    @Test
    void returnsFinalWithoutToolCall() {
        ReactRunner runner = runnerWithResponses("""
                {"action":"FINAL","answer":"直接结束"}
                """);

        ReactRunResult out = runner.runDetailed(spec(List.of(), 3));

        assertThat(out.answer()).isEqualTo("直接结束");
        assertThat(out.converged()).isTrue();
        assertThat(out.finishReason()).isEqualTo("final");
    }

    @Test
    void executesToolAndContinuesUntilFinal() {
        AtomicInteger calls = new AtomicInteger();
        ReactTool tool = new ReactTool() {
            @Override
            public String name() {
                return "docker_skill";
            }

            @Override
            public String description() {
                return "docker";
            }

            @Override
            public String execute(Map<String, Object> args, Map<String, Object> context) {
                calls.incrementAndGet();
                return "{\"status\":\"ok\",\"message\":\"found\"}";
            }
        };
        ReactRunner runner = runnerWithResponses(
                """
                {"action":"CALL_TOOL","tool":"docker_skill","args":{"task":"查容器"}}
                """,
                """
                {"action":"FINAL","answer":"容器存在"}
                """);

        ReactRunResult out = runner.runDetailed(spec(List.of(tool), 3));

        assertThat(out.answer()).isEqualTo("容器存在");
        assertThat(out.converged()).isTrue();
        assertThat(calls).hasValue(1);
    }

    @Test
    void promptsAgainForInvalidJsonAndStopsAtMaxIters() {
        ReactRunner runner = runnerWithResponses("not-json", "still-not-json");

        ReactRunResult out = runner.runDetailed(spec(List.of(), 2));

        assertThat(out.answer()).isEqualTo("TestAgent达到最大轮次 (2)，未收敛。");
        assertThat(out.converged()).isFalse();
        assertThat(out.finishReason()).isEqualTo("max_iters");
    }

    @Test
    void recordsReactMetricsOnConvergedRun() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OpsAiMetrics metrics = new OpsAiMetrics(registry);
        ReactTool tool = simpleTool("docker_skill", new AtomicInteger());
        ReactRunner runner = runnerWith(metrics,
                """
                {"action":"CALL_TOOL","tool":"docker_skill","args":{"task":"查容器"}}
                """,
                """
                {"action":"FINAL","answer":"容器存在"}
                """);

        runner.runDetailed(spec(List.of(tool), 3));

        var iterations = registry.get("react.iterations").summary();
        assertThat(iterations.count()).isEqualTo(1);
        assertThat(iterations.max()).isEqualTo(2.0); // FINAL 在第 2 轮
    }

    @Test
    void recordsRetryAndUnconvergedMetricsOnMaxIters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OpsAiMetrics metrics = new OpsAiMetrics(registry);
        ReactRunner runner = runnerWith(metrics, "not-json", "still-not-json");

        runner.runDetailed(spec(List.of(), 2));

        assertThat(registry.get("react.parse.retry").counter().count()).isEqualTo(2.0);
        assertThat(registry.get("react.unconverged").counter().count()).isEqualTo(1.0);
    }

    private static ReactTool simpleTool(String name, AtomicInteger calls) {
        return new ReactTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return name;
            }

            @Override
            public String execute(Map<String, Object> args, Map<String, Object> context) {
                calls.incrementAndGet();
                return "{\"status\":\"ok\",\"message\":\"found\"}";
            }
        };
    }

    private static ReactAgentSpec spec(List<ReactTool> tools, int maxIters) {
        return new ReactAgentSpec(
                "TestAgent",
                "test-agent",
                "system",
                "user",
                Map.of(),
                tools,
                maxIters);
    }

    private static ReactRunner runnerWithResponses(String... responses) {
        return runnerWith(new OpsAiMetrics(new SimpleMeterRegistry()), responses);
    }

    private static ReactRunner runnerWith(OpsAiMetrics metrics, String... responses) {
        LlmCallTracer tracer = mock(LlmCallTracer.class);
        when(tracer.chatText(anyString(), anyString(), any())).thenAnswer(inv -> {
            Supplier<ChatResponse> supplier = inv.getArgument(2);
            return supplier.get().getResult().getOutput().getText();
        });
        return new ReactRunner(new QueueChatModel(responses), tracer, metrics);
    }

    private static class QueueChatModel implements ChatModel {

        private final Queue<String> responses = new ArrayDeque<>();

        QueueChatModel(String... responses) {
            this.responses.addAll(List.of(responses));
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            String text = responses.isEmpty() ? "{\"action\":\"FINAL\",\"answer\":\"\"}" : responses.remove();
            return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        }
    }
}
