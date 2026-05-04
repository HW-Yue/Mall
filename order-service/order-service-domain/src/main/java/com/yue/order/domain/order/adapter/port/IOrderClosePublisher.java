package com.yue.order.domain.order.adapter.port;

/**
 * 订单关单事件发布端口（出站，由 infrastructure 层实现）
 */
public interface IOrderClosePublisher {

    /**
     * 发布关单事件
     * → order-close-normal     被 pay-service 消费
     * → order-close-group-buy  被 pay-service 消费
     * → order-close-seckill    被 pay-service 与 seckill-service 消费
     */
    void publishOrderClose(String userId, String orderId, String outTradeNo, String marketType);
}
