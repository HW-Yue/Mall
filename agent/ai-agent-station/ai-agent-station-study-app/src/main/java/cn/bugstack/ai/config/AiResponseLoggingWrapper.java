package cn.bugstack.ai.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 包装 ClientHttpResponse，在首次读取 body 时把完整响应体（含被框架“消化”的第一次回复，如 tool_calls）打印到日志。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 */
@Slf4j
public class AiResponseLoggingWrapper implements ClientHttpResponse {

    private final ClientHttpResponse delegate;
    private byte[] bodyCache;
    private boolean bodyLogged;

    public AiResponseLoggingWrapper(ClientHttpResponse delegate) {
        this.delegate = delegate;
    }

    @Override
    public HttpStatusCode getStatusCode() throws IOException {
        return delegate.getStatusCode();
    }

    @Override
    public String getStatusText() throws IOException {
        return delegate.getStatusText();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public InputStream getBody() throws IOException {
        if (bodyCache == null) {
            InputStream raw = delegate.getBody();
            bodyCache = raw.readAllBytes();
            raw.close();
        }
        if (!bodyLogged && bodyCache != null && bodyCache.length > 0) {
            bodyLogged = true;
            String bodyStr = new String(bodyCache, StandardCharsets.UTF_8);
            Object bodyObj;
            try {
                bodyObj = JSON.parse(bodyStr);
            } catch (Exception e) {
                bodyObj = bodyStr;
            }
            String json = JSON.toJSONString(bodyObj, SerializerFeature.PrettyFormat);
            log.info("[Spring AI Response] 完整响应体（含被框架处理的第一次回复/tool_calls 等）:\n{}", json);
        }
        return new ByteArrayInputStream(bodyCache);
    }

    @Override
    public HttpHeaders getHeaders() {
        return delegate.getHeaders();
    }
}
