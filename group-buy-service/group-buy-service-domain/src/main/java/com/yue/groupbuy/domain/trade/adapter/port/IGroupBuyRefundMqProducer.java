package com.yue.groupbuy.domain.trade.adapter.port;

/**
 * 拼团退款/关单 MQ 生产者端口（由 infrastructure 层实现）
 */
public interface IGroupBuyRefundMqProducer {

    /**
     * 发送未支付订单关单消息（order-close-group-buy）
     */
    void sendOrderCloseMessage(String outTradeNo, String userId);

    /**
     * 发送已支付订单退款消息（pay-refund-group-buy）
     */
    void sendPayRefundMessage(String outTradeNo, String userId);

}
