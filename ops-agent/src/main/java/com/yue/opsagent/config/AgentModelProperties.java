package com.yue.opsagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent LLM 模型配置
 * 绑定 application.yml 中的 ops-agent.model 前缀
 */
@Component
@ConfigurationProperties(prefix = "ops-agent.model")
public class AgentModelProperties {

    private String provider = "dashscope";
    private String apiKey;
    private String modelName = "qwen-max";
    private String baseUrl;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
