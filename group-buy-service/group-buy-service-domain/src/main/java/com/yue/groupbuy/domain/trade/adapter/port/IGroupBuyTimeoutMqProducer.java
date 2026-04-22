package com.yue.groupbuy.domain.trade.adapter.port;

/**
 * 拼团队级超时事实 MQ 生产端口。
 */
public interface IGroupBuyTimeoutMqProducer {

    /**
     * 发送拼团队级超时消息。
     */
    void sendTimeoutRefundMessage(String teamId, long deliverTimeMs);
}
