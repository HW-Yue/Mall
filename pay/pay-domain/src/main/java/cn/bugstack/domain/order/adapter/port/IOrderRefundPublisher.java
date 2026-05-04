package cn.bugstack.domain.order.adapter.port;

/**
 * 订单退款消息发布接口（出站，由 infrastructure 层实现）
 */
public interface IOrderRefundPublisher {

    /**
     * 发布订单退款消息
     */
    void sendPayRefundMessage(String userId, String outTradeNo, String marketType);
}
