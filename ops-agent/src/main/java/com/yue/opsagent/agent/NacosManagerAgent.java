package com.yue.opsagent.agent;

import com.yue.opsagent.adapter.tool.NacosConfigTool;
import com.yue.opsagent.adapter.tool.NacosNamingTool;
import com.yue.opsagent.config.AgentModelProperties;
import com.yue.opsagent.config.AgentSystemPromptProperties;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.tool.Toolkit;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Domain Agent：装配层
 * Spring DI 注入所有 Tool 和配置，组装成 ReActAgent
 * 对外暴露 createAgent() 作为工厂方法，供 AG-UI 使用
 */
@Component
public class NacosManagerAgent {

    private final NacosConfigTool configTool;
    private final NacosNamingTool namingTool;
    private final AgentModelProperties modelProps;
    private final AgentSystemPromptProperties promptProps;
    private final List<Hook> hooks;

    public NacosManagerAgent(NacosConfigTool configTool,
                             NacosNamingTool namingTool,
                             AgentModelProperties modelProps,
                             AgentSystemPromptProperties promptProps,
                             List<Hook> hooks) {
        this.configTool = configTool;
        this.namingTool = namingTool;
        this.modelProps = modelProps;
        this.promptProps = promptProps;
        this.hooks = hooks;
    }

    /**
     * 工厂方法：默认画像（AG-UI 对话场景）。每次调用创建新的 ReActAgent 实例，
     * 保证会话隔离，工具实例复用（Spring 单例）。
     */
    public ReActAgent createAgent() {
        return createAgent(promptProps.getSystemPrompt(), "nacos-manager");
    }

    /**
     * 工厂方法：可定制画像。告警策略用此重载为每个域（sentinel / dtp / ...）建一个
     * 独立 Agent，传入各域的 system prompt 与 agent name。
     *
     * @param sysPrompt 该 Agent 的 system prompt（如 sentinel 调优 prompt）
     * @param agentName 供观测区分，例如 "sentinel-ops" / "dtp-ops"
     */
    public ReActAgent createAgent(String sysPrompt, String agentName) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(configTool);
        toolkit.registerTool(namingTool);

        ReActAgent.Builder builder = ReActAgent.builder()
                .name(agentName == null || agentName.isBlank() ? "nacos-manager" : agentName)
                .sysPrompt(sysPrompt == null || sysPrompt.isBlank() ? promptProps.getSystemPrompt() : sysPrompt)
                .model(createModel())
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .maxIters(10);

        if (hooks != null && !hooks.isEmpty()) {
            builder.hooks(hooks);
        }

        return builder.build();
    }

    private Model createModel() {
        String provider = modelProps.getProvider();
        String apiKey = modelProps.getApiKey();
        String modelName = modelProps.getModelName();
        String baseUrl = modelProps.getBaseUrl();

        if ("openai".equalsIgnoreCase(provider)) {
            var builder = OpenAIChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName);
            if (baseUrl != null && !baseUrl.isEmpty()) {
                builder.baseUrl(baseUrl);
            }
            return builder.build();
        }

        // 默认 dashscope
        var builder = DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .stream(true)
                .formatter(new DashScopeChatFormatter());
        if (baseUrl != null && !baseUrl.isEmpty()) {
            builder.baseUrl(baseUrl);
        }
        return builder.build();
    }
}
