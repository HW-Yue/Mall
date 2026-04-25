package com.yue.infrastructure.feign;

import feign.RequestInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 调用 order-service 时可选携带与订单服务 app.order.internal-create-token 一致的凭证
 */
@Configuration
public class OrderServiceFeignConfig {

    @Value("${app.order.internal-create-token:}")
    private String internalCreateToken;

    @Bean
    public RequestInterceptor orderInternalTokenInterceptor() {
        return template -> {
            if (StringUtils.isNotBlank(internalCreateToken)) {
                template.header("X-Internal-Token", internalCreateToken);
            }
        };
    }
}
