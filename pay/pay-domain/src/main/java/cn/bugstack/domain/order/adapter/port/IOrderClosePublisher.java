package cn.bugstack.domain.order.adapter.port;

/**
 * 订单关单消息发布接口（出站，由 infrastructure 层实现）
 */
public interface IOrderClosePublisher {

    /**
     * 发布订单关单消息
     */
    void sendOrderCloseMessage(String userId, String outTradeNo, String marketType);
}
