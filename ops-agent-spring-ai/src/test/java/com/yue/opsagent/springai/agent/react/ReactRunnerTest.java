package com.yue.opsagent.springai.agent.react;

import com.yue.opsagent.springai.infrastructure.observability.LlmCallTracer;
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

        String out = runner.run(spec(List.of(), 3));

        assertThat(out).isEqualTo("直接结束");
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

        String out = runner.run(spec(List.of(tool), 3));

        assertThat(out).isEqualTo("容器存在");
        assertThat(calls).hasValue(1);
    }

    @Test
    void promptsAgainForInvalidJsonAndStopsAtMaxIters() {
        ReactRunner runner = runnerWithResponses("not-json", "still-not-json");

        String out = runner.run(spec(List.of(), 2));

        assertThat(out).isEqualTo("TestAgent达到最大轮次 (2)，未收敛。");
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
        LlmCallTracer tracer = mock(LlmCallTracer.class);
        when(tracer.chatText(anyString(), anyString(), any())).thenAnswer(inv -> {
            Supplier<ChatResponse> supplier = inv.getArgument(2);
            return supplier.get().getResult().getOutput().getText();
        });
        return new ReactRunner(new QueueChatModel(responses), tracer);
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
