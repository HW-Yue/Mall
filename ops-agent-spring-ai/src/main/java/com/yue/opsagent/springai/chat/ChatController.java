package com.yue.opsagent.springai.chat;

import com.yue.opsagent.springai.agent.parent.ParentReactAgent;
import com.yue.opsagent.springai.config.OpsAiProperties;
import com.yue.opsagent.springai.skill.registry.MasterRegistry;
import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private static final String SYSTEM = """
            你是运维助手。根据用户问题选择并说明可用的 Skill（见下列菜单），必要时可建议调用工具名与参数。
            当前仅通过对话协助；如需执行工具，可提示用户调用 HTTP API POST /api/v1/tools/execute，body JSON:
            {"skill":"技能名","tool":"工具名","args":{...}}。

            可用 Skill：
            """;

    private final ChatClient chatClient;
    private final MasterRegistry masterRegistry;
    private final ParentReactAgent parentReactAgent;
    private final OpsAiProperties opsAiProperties;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ChatController(
            ChatClient.Builder chatClientBuilder,
            MasterRegistry masterRegistry,
            ParentReactAgent parentReactAgent,
            OpsAiProperties opsAiProperties) {
        this.chatClient = chatClientBuilder.build();
        this.masterRegistry = masterRegistry;
        this.parentReactAgent = parentReactAgent;
        this.opsAiProperties = opsAiProperties;
    }

    /**
     * 父 Agent ReAct + 子域委派工具（阻塞一次返回，适合调试；流式见 /stream + ops-ai.chat.mode）。
     */
    @PostMapping("/react")
    public Map<String, String> react(@Valid @RequestBody ChatStreamRequest request) {
        return Map.of("reply", parentReactAgent.chat(request.message()));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatStreamRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);
        if ("react".equalsIgnoreCase(opsAiProperties.getChat().getMode())) {
            Flux<String> flux = Flux.just(parentReactAgent.chat(request.message()));
            flux.subscribe(
                    chunk -> executor.execute(() -> {
                        try {
                            emitter.send(SseEmitter.event().data(chunk));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }),
                    emitter::completeWithError,
                    emitter::complete);
            return emitter;
        }
        String system = SYSTEM + masterRegistry.buildMenu();
        Flux<String> flux = chatClient.prompt()
                .system(system)
                .user(request.message())
                .stream()
                .content();
        flux.subscribe(
                chunk -> executor.execute(() -> {
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }),
                emitter::completeWithError,
                emitter::complete
        );
        return emitter;
    }
}
