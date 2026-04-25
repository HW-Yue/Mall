package com.yue.infrastructure.feign;

import com.yue.order.api.dto.CreateOrderRequestDTO;
import com.yue.order.api.dto.CreateOrderResponseDTO;
import com.yue.order.api.response.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 订单服务：普通品 mall 已锁库后的异步建单入口
 */
@FeignClient(
        name = "order-service",
        path = "/api/v1/order",
        contextId = "orderServiceForMall",
        configuration = OrderServiceFeignConfig.class
)
public interface IOrderServiceForMallFeign {

    @PostMapping("/create_order_normal_from_mall")
    Response<CreateOrderResponseDTO> createOrderNormalFromMall(@RequestBody CreateOrderRequestDTO request);
}
