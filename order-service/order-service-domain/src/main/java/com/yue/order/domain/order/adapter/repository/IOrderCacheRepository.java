package com.yue.order.domain.order.adapter.repository;

import java.time.Duration;

/**
 * 订单创建链路 Redis 存在标记：发 MQ 前写入，消费者落库后清理。
 * 用于 get_pay_url 在 t_order 尚未异步落库时快速判断订单是否真实在飞。
 */
public interface IOrderCacheRepository {

    void markPending(String userId, String orderId, String outTradeNo, Duration ttl);

    boolean existsPending(String userId, String orderId);

    void clearPending(String userId, String orderId);
}
