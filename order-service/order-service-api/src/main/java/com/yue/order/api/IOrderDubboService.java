package com.yue.order.api;

import com.yue.order.api.dto.CreateOrderRequestDTO;
import com.yue.order.api.dto.CreateOrderResponseDTO;
import com.yue.order.api.dto.RefundRequestDTO;

/**
 * 订单服务 Dubbo RPC 接口（Triple 协议）
 * 失败时抛异常，成功时直接返回 DTO
 */
public interface IOrderDubboService {

    CreateOrderResponseDTO createOrder(CreateOrderRequestDTO request);

    CreateOrderResponseDTO createOrderNormalFromMall(CreateOrderRequestDTO request);

    Boolean refundExecute(RefundRequestDTO request);
}
