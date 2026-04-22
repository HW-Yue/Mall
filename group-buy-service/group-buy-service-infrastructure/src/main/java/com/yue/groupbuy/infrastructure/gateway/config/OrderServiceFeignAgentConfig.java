package com.yue.groupbuy.infrastructure.gateway.config;

import com.yue.groupbuy.infrastructure.config.AgentRuntimeProperties;
import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 将 {@link AgentRuntimeProperties} 中的 Feign 超时绑定为 {@link Request.Options}。
 * <p>
 * 不在此使用 {@code @RefreshScope}：Feign 客户端配置类运行在 {@code NamedContextFactory} 子上下文中，
 * 该上下文未注册 {@code refresh} 作用域，会导致启动失败（{@code No Scope registered for scope name 'refresh'}）。
 */
@Configuration
public class OrderServiceFeignAgentConfig {

    @Bean
    public Request.Options orderServiceFeignOptions(AgentRuntimeProperties agentRuntimeProperties) {
        AgentRuntimeProperties.Feign.OrderService o = agentRuntimeProperties.getFeign().getOrderService();
        return new Request.Options(
                Duration.ofMillis(o.getConnectTimeoutMs()),
                Duration.ofMillis(o.getReadTimeoutMs()),
                true);
    }
}
