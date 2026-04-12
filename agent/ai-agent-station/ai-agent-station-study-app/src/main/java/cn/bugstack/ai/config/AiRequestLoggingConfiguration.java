package cn.bugstack.ai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;

/**
 * 仅在 dev/test 环境下注册带请求日志的 RestClient.Builder 与 WebClient.Builder，
 * 供 Spring AI OpenAiApi 使用，便于调试时查看发往 LLM 的完整请求体（含同步与流式）。
 * 生产环境不注册，避免日志溢出。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 */
@Configuration
@Profile({"dev", "test"})
@ConditionalOnProperty(prefix = "spring.ai.request-logging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiRequestLoggingConfiguration {

    public static final String AI_REQUEST_LOGGING_REST_CLIENT_BUILDER = "aiRequestLoggingRestClientBuilder";
    public static final String AI_REQUEST_LOGGING_WEB_CLIENT_BUILDER = "aiRequestLoggingWebClientBuilder";

    @Bean(AI_REQUEST_LOGGING_REST_CLIENT_BUILDER)
    public RestClient.Builder aiRequestLoggingRestClientBuilder() {
        AiRequestLoggingInterceptor interceptor = new AiRequestLoggingInterceptor();
        InterceptingClientHttpRequestFactory requestFactory = new InterceptingClientHttpRequestFactory(
                new SimpleClientHttpRequestFactory(),
                Collections.singletonList(interceptor));

        return RestClient.builder()
                .requestFactory(requestFactory);
    }

    /**
     * 仅当 classpath 存在 reactor-netty 时注册（否则会 NoClassDefFoundError: io/netty/util/AttributeKey）。
     * 未注册时 AiClientApiNode 中 resolveLoggingWebClientBuilder() 返回 null，仅同步请求被拦截，流式不拦截。
     */
    @Bean(AI_REQUEST_LOGGING_WEB_CLIENT_BUILDER)
    @ConditionalOnClass(name = "reactor.netty.http.client.HttpClient")
    public WebClient.Builder aiRequestLoggingWebClientBuilder() {
        return WebClient.builder()
                .clientConnector(new AiRequestLoggingClientHttpConnector());
    }
}
