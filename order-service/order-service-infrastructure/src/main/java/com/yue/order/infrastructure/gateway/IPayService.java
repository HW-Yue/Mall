package com.yue.order.infrastructure.gateway;

import com.yue.order.infrastructure.gateway.dto.CreatePayRequestDTO;
import com.yue.order.infrastructure.gateway.response.GatewayResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 支付服务 Feign 客户端（对应 pay-service 服务）
 */
@FeignClient(name = "pay-service")
public interface IPayService {

    @PostMapping("/api/v1/alipay/create_pay_order")
    GatewayResponse<String> createPayOrder(@RequestBody CreatePayRequestDTO requestDTO);
}
