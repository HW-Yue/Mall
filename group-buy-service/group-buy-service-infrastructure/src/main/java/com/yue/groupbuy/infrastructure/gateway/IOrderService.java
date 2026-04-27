package com.yue.groupbuy.infrastructure.gateway;

import com.yue.groupbuy.infrastructure.gateway.dto.CreateOrderRequestDTO;
import com.yue.groupbuy.infrastructure.gateway.dto.CreateOrderResponseDTO;
import com.yue.groupbuy.infrastructure.gateway.dto.GatewayResponse;
import com.yue.groupbuy.infrastructure.gateway.dto.QueryOrderByOutTradeNoRequestDTO;
import com.yue.groupbuy.infrastructure.gateway.dto.RefundRequestDTO;
import com.yue.groupbuy.infrastructure.gateway.config.OrderServiceFeignAgentConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "order-service", configuration = OrderServiceFeignAgentConfig.class)
public interface IOrderService {

    @PostMapping("/api/v1/order/create_order")
    GatewayResponse<CreateOrderResponseDTO> createOrder(@RequestBody CreateOrderRequestDTO request);

    @PostMapping("/api/v1/order/query_order_by_out_trade_no")
    GatewayResponse<CreateOrderResponseDTO> queryOrderByOutTradeNo(@RequestBody QueryOrderByOutTradeNoRequestDTO request);

    @PostMapping("/api/v1/order/refund_execute")
    GatewayResponse<Boolean> refundExecute(@RequestBody RefundRequestDTO request);
}
