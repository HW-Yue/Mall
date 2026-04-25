package com.yue.order.domain.order.adapter.port;

/**
 * 普通商品：mall 已锁库后，将订单全量落库信息同步投递 MQ（由消费者 INSERT）
 */
public interface INormalOrderPendingPublisher {

    /**
     * 同步投递，发送失败时抛出异常由上层触发 mall 解锁库存
     */
    void publishInsertSync(String messageBody);
}
