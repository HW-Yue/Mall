package com.yue.opsagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * MCP 子 Agent 相关配置，绑定 {@code ops-agent.mcp.*}。
 *
 * <p>目前接入的只有 Elasticsearch MCP Server（官方 {@code docker.elastic.co/mcp/elasticsearch}，
 * streamable-HTTP 模式监听 :8080/mcp）。后续若再接 Prometheus / Datadog / Grafana 等 MCP，
 * 在本类继续按 server 名加一个 inner properties 即可。</p>
 *
 * <pre>
 * ops-agent:
 *   mcp:
 *     elasticsearch:
 *       enabled: true
 *       transport: sse
 *       url: http://127.0.0.1:8085/mcp/sse
 *       timeout: PT20S
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "ops-agent.mcp")
public class McpProperties {

    private Elasticsearch elasticsearch = new Elasticsearch();

    public Elasticsearch getElasticsearch() {
        return elasticsearch;
    }

    public void setElasticsearch(Elasticsearch elasticsearch) {
        this.elasticsearch = elasticsearch;
    }

    /** Elasticsearch MCP Server 连接参数。 */
    public static class Elasticsearch {

        /** 开关。false 时 DiagnoseAgent 降级为空，策略按"仅落 inbox 不诊断"处理。 */
        private boolean enabled = false;

        /**
         * 与 Elasticsearch MCP 容器之间的 MCP 传输方式。
         * <ul>
         *   <li>{@code sse}（默认）— 走官方镜像提供的 {@code /mcp/sse}（HTTP+SSE 旧版传输），
         *       与 AgentScope {@code sseTransport} 对齐；可避免部分环境下 streamable-HTTP
         *       在「无 Mcp-Session-Id 的 GET」上返回 401 的问题。</li>
         *   <li>{@code streamable-http} — {@code /mcp}，需服务端与 mcp-core 0.17.x 会话语义一致。</li>
         * </ul>
         */
        private Transport transport = Transport.SSE;

        /**
         * MCP 端点 URL，须与 {@link #transport} 一致：
         * <ul>
         *   <li>{@code sse} → {@code http://host:port/mcp/sse}</li>
         *   <li>{@code streamable-http} → {@code http://host:port/mcp}</li>
         * </ul>
         */
        private String url = "http://127.0.0.1:8085/mcp/sse";

        /** 单次 MCP 请求超时；ES search 有时要 5~10s，给 20s 较稳妥。 */
        private Duration timeout = Duration.ofSeconds(20);

        /** 初始化阶段的 listTools / handshake 超时。 */
        private Duration initTimeout = Duration.ofSeconds(10);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Transport getTransport() {
            return transport;
        }

        public void setTransport(Transport transport) {
            this.transport = transport;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public Duration getInitTimeout() {
            return initTimeout;
        }

        public void setInitTimeout(Duration initTimeout) {
            this.initTimeout = initTimeout;
        }
    }

    /** {@link Elasticsearch#transport} 的合法取值。 */
    public enum Transport {
        SSE,
        STREAMABLE_HTTP
    }
}
