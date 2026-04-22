package com.yue.opsagent.agent;

import com.yue.opsagent.config.AgentModelProperties;
import com.yue.opsagent.config.AgentSystemPromptProperties;
import com.yue.opsagent.config.McpProperties;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 诊断子 Agent 工厂：通过 MCP 协议接入 {@code docker.elastic.co/mcp/elasticsearch}，
 * 让 LLM 可以用自然语言执行 {@code list_indices / get_mappings / search / esql / get_shards}
 * 等只读工具查询 Nexus 的全链路日志（nexus-<service>-yyyy.MM.dd 索引）。
 *
 * <p>与 {@link NacosManagerAgent} 风格一致：每次 {@link #createAgent(String, String)}
 * 返回一个新的 {@link ReActAgent} 实例（独立 memory），但共享同一个已经初始化好的
 * Toolkit（避免每次建 Agent 都重新和 MCP Server 握手 / 拉工具表）。</p>
 *
 * <p>与 Nacos 工具链保持隔离：{@link Toolkit} 里只注册 ES MCP client，诊断场景下
 * 不允许 Agent 调 {@code publishConfig}（防止改配置动作从诊断通路漏出）。</p>
 *
 * <h3>初始化时序</h3>
 * <ol>
 *   <li>Spring 启动完成后 {@link #onApplicationReady} 异步拉 MCP client 与工具列表；</li>
 *   <li>失败只 log.warn 不抛异常，不影响主流程。失败后 {@link #isReady()} 返回 false，
 *       调用方应该降级为"仅落 inbox、reasoning=[诊断失败]"。</li>
 * </ol>
 */
@Component
public class DiagnoseAgent {

    private static final Logger log = LoggerFactory.getLogger(DiagnoseAgent.class);

    private final AgentModelProperties modelProps;
    private final AgentSystemPromptProperties promptProps;
    private final McpProperties mcpProps;

    /** 预初始化的 Toolkit；init 成功后赋值，所有 createAgent 共享。 */
    private volatile Toolkit cachedToolkit;
    /** 保存 wrapper 用于应用关闭时释放；同时作为 isReady 的判定。 */
    private volatile McpClientWrapper cachedWrapper;

    public DiagnoseAgent(AgentModelProperties modelProps,
                         AgentSystemPromptProperties promptProps,
                         McpProperties mcpProps) {
        this.modelProps = modelProps;
        this.promptProps = promptProps;
        this.mcpProps = mcpProps;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        McpProperties.Elasticsearch es = mcpProps.getElasticsearch();
        if (!es.isEnabled()) {
            log.info("[DiagnoseAgent] ES MCP 未启用（ops-agent.mcp.elasticsearch.enabled=false），诊断子 Agent 将降级");
            return;
        }
        String url = es.getUrl();
        log.info("[DiagnoseAgent] init ES MCP client transport={} url={} timeout={} initTimeout={}",
                es.getTransport(), url, es.getTimeout(), es.getInitTimeout());

        McpClientBuilder builder = McpClientBuilder.create("elasticsearch-mcp")
                .timeout(es.getTimeout())
                .initializationTimeout(es.getInitTimeout());
        builder = switch (es.getTransport()) {
            case SSE -> builder.sseTransport(url);
            case STREAMABLE_HTTP -> builder.streamableHttpTransport(url);
        };

        builder.buildAsync()
                .flatMap(wrapper -> {
                    cachedWrapper = wrapper;
                    Toolkit tk = new Toolkit();
                    return tk.registerMcpClient(wrapper).thenReturn(tk);
                })
                .subscribe(
                        tk -> {
                            cachedToolkit = tk;
                            log.info("[DiagnoseAgent] ES MCP ready tools={}", tk.getToolNames());
                        },
                        err -> log.warn("[DiagnoseAgent] ES MCP 初始化失败，诊断子 Agent 降级为不可用: {}",
                                err.toString(), err));
    }

    @PreDestroy
    public void shutdown() {
        McpClientWrapper w = cachedWrapper;
        if (w != null) {
            try {
                w.close();
            } catch (Exception ex) {
                log.warn("[DiagnoseAgent] close mcp client failed: {}", ex.toString());
            }
        }
    }

    /**
     * 诊断子 Agent 是否可用：true 表示 MCP 握手完成并拿到工具列表，子类可调 {@link #createAgent}。
     * false 时调用方应走降级路径（不诊断，直接写 [诊断失败] 等）。
     */
    public boolean isReady() {
        return cachedToolkit != null;
    }

    /**
     * 工厂方法：按指定 system prompt / agent name 建一个诊断 Agent。当 MCP 未就绪时返回 null，
     * 调用方务必做 null 判断以便降级。
     *
     * @param sysPrompt 本次诊断的 system prompt（通常来自 {@code ops-agent.prompts.redis-diagnose}）
     * @param agentName 观测用名字，例如 {@code "redis-diagnose"}
     */
    public ReActAgent createAgent(String sysPrompt, String agentName) {
        Toolkit tk = cachedToolkit;
        if (tk == null) {
            log.warn("[DiagnoseAgent] toolkit 未初始化，无法创建诊断 Agent agentName={}", agentName);
            return null;
        }
        String finalPrompt = (sysPrompt == null || sysPrompt.isBlank())
                ? promptProps.getSystemPrompt() : sysPrompt;
        String finalName = (agentName == null || agentName.isBlank()) ? "diagnose" : agentName;

        return ReActAgent.builder()
                .name(finalName)
                .sysPrompt(finalPrompt)
                .model(createModel())
                .toolkit(tk)
                .memory(new InMemoryMemory())
                .maxIters(8)
                .build();
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
