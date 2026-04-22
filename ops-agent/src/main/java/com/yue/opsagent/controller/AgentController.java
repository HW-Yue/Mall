package com.yue.opsagent.controller;

import com.yue.opsagent.agent.NacosManagerAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * 入站适配器：REST API 层
 * 用户通过 HTTP 和 Agent 交互（兼容 AG-UI 之外的手动调用）
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final NacosManagerAgent agentFactory;

    public AgentController(NacosManagerAgent agentFactory) {
        this.agentFactory = agentFactory;
    }

    @PostMapping("/chat")
    public Mono<String> chat(@RequestBody String userInput) {
        var agent = agentFactory.createAgent();
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .textContent(userInput)
                .build();
        return agent.call(userMsg)
                .map(Msg::getTextContent);
    }
}
