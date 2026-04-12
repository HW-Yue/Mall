package cn.bugstack.ai.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 装饰 WebClient 的 ClientHttpRequest，在 writeWith 时拦截并打印流式请求的 URL、Headers、Body。
 * 用于 dev/test 下查看发往 LLM 的完整请求体（含流式 chat completions）。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 */
@Slf4j
public class AiRequestLoggingWebClientDecorator extends ClientHttpRequestDecorator {

    public AiRequestLoggingWebClientDecorator(ClientHttpRequest delegate) {
        super(delegate);
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        return DataBufferUtils.join(body)
                .flatMap(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);

                    Map<String, Object> logPayload = AiRequestLoggingInterceptor.buildLogPayload(
                            getURI().toString(),
                            getMethod().name(),
                            getHeaders().toSingleValueMap(),
                            bytes);

                    String json = JSON.toJSONString(logPayload, SerializerFeature.PrettyFormat);
                    log.info("[Spring AI Request] 完整请求体 (WebClient/流式):\n{}", json);

                    DataBuffer out = getDelegate().bufferFactory().wrap(bytes);
                    return getDelegate().writeWith(Mono.just(out));
                });
    }
}
