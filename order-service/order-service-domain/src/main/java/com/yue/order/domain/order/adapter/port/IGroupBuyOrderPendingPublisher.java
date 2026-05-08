package com.yue.order.domain.order.adapter.port;

/**
 * 拼团商品异步落单：发 MQ 由消费者 INSERT t_order，避免在 createOrder 同步路径下持有 DB 事务。
 */
public interface IGroupBuyOrderPendingPublisher {

    /**
     * 同步投递，发送失败抛异常由上层补偿（删除 Redis 存在标记 + 抛出业务异常）
     */
    void publishInsertSync(String messageBody);
}
