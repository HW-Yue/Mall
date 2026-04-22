package com.yue.opsagent.config;

import com.yue.opsagent.adapter.hook.ApprovalInterceptorHook;
import com.yue.opsagent.agent.NacosManagerAgent;
import io.agentscope.spring.boot.agui.common.AguiAgentRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AG-UI 智能体注册配置
 * 将 NacosManagerAgent 暴露为 AG-UI 协议端点
 */
@Configuration
public class AguiAgentConfig {

    /**
     * 审批拦截 hook（priority=20）。由 {@link NacosManagerAgent#createAgent()}
     * 通过 Spring 注入的 {@code List<Hook>} 自动装配进 ReActAgent。
     */
    @Bean
    public ApprovalInterceptorHook approvalInterceptorHook(ApprovalProperties approvalProperties) {
        return new ApprovalInterceptorHook(approvalProperties);
    }

    @Bean
    public AguiAgentRegistryCustomizer aguiAgentRegistryCustomizer(NacosManagerAgent nacosManagerAgent) {
        return registry -> registry.registerFactory("nacos", nacosManagerAgent::createAgent);
    }
}
