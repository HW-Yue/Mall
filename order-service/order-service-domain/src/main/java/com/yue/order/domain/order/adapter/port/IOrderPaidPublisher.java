package com.yue.order.domain.order.adapter.port;

import java.util.Date;

/**
 * 订单支付成功事件发布端口（出站，由 infrastructure 层实现）
 */
public interface IOrderPaidPublisher {

    /**
     * 发布支付成功事件
     * → order-paid-normal     被 order-service 消费
     * → order-paid-group_buy  被 group-buy-service 消费
     * → order-paid-seckill    被 seckill-service 消费
     */
    void publishOrderPaid(String userId, String orderId, String outTradeNo, String marketType, Date outTradeTime);
}
