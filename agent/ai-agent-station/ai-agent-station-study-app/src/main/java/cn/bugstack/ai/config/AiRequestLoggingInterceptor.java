package cn.bugstack.ai.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 拦截发往 AI 服务的 HTTP 请求，将 URL、Headers、Request Body 以格式化 JSON 输出到日志。
 * 仅在 dev/test 环境下通过配置启用，避免生产环境日志溢出。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 */
@Slf4j
public class AiRequestLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String MASKED = "***";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        Map<String, Object> logPayload = buildLogPayload(request.getURI().toString(), request.getMethod().name(),
                request.getHeaders().toSingleValueMap(), body);
        String json = JSON.toJSONString(logPayload, SerializerFeature.PrettyFormat);
        log.info("[Spring AI Request] 完整请求体 (RestClient):\n{}", json);

        ClientHttpResponse response = execution.execute(request, body);
        return new AiResponseLoggingWrapper(response);
    }

    static Map<String, Object> buildLogPayload(String url, String method, Map<String, String> rawHeaders, byte[] body) {
        Map<String, Object> logPayload = new HashMap<>();
        logPayload.put("url", url);
        logPayload.put("method", method);
        Map<String, String> headers = rawHeaders.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> maskHeader(e.getKey(), e.getValue())));
        logPayload.put("headers", headers);
        if (body != null && body.length > 0) {
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            try {
                logPayload.put("body", JSON.parse(bodyStr));
            } catch (Exception e) {
                logPayload.put("body", bodyStr);
            }
        } else {
            logPayload.put("body", null);
        }
        return logPayload;
    }

    static String maskHeader(String name, String value) {
        if (name == null || value == null) {
            return value;
        }
        if (AUTHORIZATION.equalsIgnoreCase(name)) {
            return MASKED;
        }
        return value;
    }
}
