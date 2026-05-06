package com.yue.order.api;

import com.yue.order.api.dto.CreateOrderRequestDTO;
import com.yue.order.api.dto.CreateOrderResponseDTO;
import com.yue.order.api.dto.QueryOrderByOutTradeNoRequestDTO;
import com.yue.order.api.dto.RefundRequestDTO;

/**
 * 订单服务 Dubbo RPC 接口（Triple 协议）
 * 失败时抛异常，成功时直接返回 DTO
 */
public interface IOrderDubboService {

    CreateOrderResponseDTO createOrder(CreateOrderRequestDTO request);

    CreateOrderResponseDTO createOrderNormalFromMall(CreateOrderRequestDTO request);

    /** 未找到订单时返回 null，系统异常时抛异常 */
    CreateOrderResponseDTO queryOrderByOutTradeNo(QueryOrderByOutTradeNoRequestDTO request);

    Boolean refundExecute(RefundRequestDTO request);
}
