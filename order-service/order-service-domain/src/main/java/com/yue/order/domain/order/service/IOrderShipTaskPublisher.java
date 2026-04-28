package com.yue.order.domain.order.service;

/**
 * 订单履约任务发布接口（出站，由 infrastructure 层实现）
 */
public interface IOrderShipTaskPublisher {

    /**
     * 发布发货任务，交给订单服务自身消费者执行发货推进
     */
    void publishOrderShipTask(String userId, String orderId, String outTradeNo);
}
