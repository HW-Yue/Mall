package cn.bugstack.ai.config;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.function.Function;

/**
 * 包装默认的 ClientHttpConnector，对每次请求用 AiRequestLoggingWebClientDecorator 装饰，
 * 从而在 dev/test 下打印 WebClient（流式）请求的完整 URL、Headers、Body。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 */
public class AiRequestLoggingClientHttpConnector implements ClientHttpConnector {

    private final ClientHttpConnector delegate = new ReactorClientHttpConnector();

    @Override
    public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
                                             Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {
        return delegate.connect(method, uri,
                request -> requestCallback.apply(new AiRequestLoggingWebClientDecorator(request)));
    }
}
