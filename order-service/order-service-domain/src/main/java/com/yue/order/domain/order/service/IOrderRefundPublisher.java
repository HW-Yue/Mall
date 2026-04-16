package com.yue.order.domain.order.service;

/**
 * 订单退款事件发布接口（出站，由 infrastructure 层实现）
 */
public interface IOrderRefundPublisher {

    /**
     * 发布退款事件
     * → pay-refund-normal     被 pay-service 消费
     * → pay-refund-group-buy  被 pay-service 消费
     * → pay-refund-seckill    被 pay-service 消费
     */
    void publishPayRefund(String userId, String outTradeNo, String marketType);

}
