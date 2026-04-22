package com.yue.order.domain.order.service;

import java.util.Date;

/**
 * 订单事件发布接口（出站，由 infrastructure 层实现）
 */
public interface IOrderEventPublisher {

    /**
     * 发布支付成功事件
     * → order-paid-normal     被 agent-service 消费（普通商品下发 Token）
     * → order-paid-group_buy  被 group-buy-service 消费（拼团结算）
     * → order-paid-seckill    被 seckill-service 消费（秒杀结算）
     */
    void publishOrderPaid(String userId, String orderId, String outTradeNo, String marketType, Date outTradeTime);

    /**
     * 发布关单事件
     * → order-close-normal     被 pay-service 消费，关闭本地支付台账
     * → order-close-group-buy  被 pay-service 消费，关闭本地支付台账
     * → order-close-seckill    被 pay-service 与 seckill-service 消费，分别关闭支付台账并恢复可售库存
     */
    void publishOrderClose(String userId, String orderId, String outTradeNo, String marketType);
}
