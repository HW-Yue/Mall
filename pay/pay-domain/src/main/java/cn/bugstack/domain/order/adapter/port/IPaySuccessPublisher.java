package cn.bugstack.domain.order.adapter.port;

import java.util.Date;

/**
 * 支付成功结算消息发布接口（出站，由 infrastructure 层实现）
 */
public interface IPaySuccessPublisher {

    /**
     * 发布支付成功结算消息
     */
    void sendSettlementMessage(String userId, String outTradeNo, Date outTradeTime, String marketType);
}
