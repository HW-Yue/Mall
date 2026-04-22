package com.yue.opsagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 系统提示词配置。绑定 {@code ops-agent.*}。
 *
 * <p>{@code systemPrompt} 作为 AG-UI 对话及兜底；{@code prompts} 按 domain 键索引，
 * 告警策略会用 {@code prompts.getOrDefault(domain(), systemPrompt)} 找到专属画像。
 * 新接域（mysql / redis / rocketmq...）时只需在 {@code application.yml} 里新增
 * {@code ops-agent.prompts.<domain>: |} 段即可，不改 Java 代码。</p>
 */
@Component
@ConfigurationProperties(prefix = "ops-agent")
public class AgentSystemPromptProperties {

    /** 默认 / AG-UI 对话场景的 system prompt，也作为策略未配置专属 prompt 时的兜底。 */
    private String systemPrompt = """
        你是一个 Nacos 运维助手，可以通过工具管理 Nacos 配置和服务发现。
        你可以：
        - 获取、发布、删除配置
        - 查看服务实例、注册和注销实例
        请用中文回复用户。
        """;

    /** 按策略域区分的 system prompt，key 与 {@link ApprovalProperties#getStrategies()} 对齐。 */
    private Map<String, String> prompts = new LinkedHashMap<>();

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public Map<String, String> getPrompts() {
        return prompts;
    }

    public void setPrompts(Map<String, String> prompts) {
        this.prompts = prompts;
    }
}
