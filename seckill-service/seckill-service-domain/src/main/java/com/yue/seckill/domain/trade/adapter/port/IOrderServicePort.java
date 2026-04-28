package com.yue.seckill.domain.trade.adapter.port;

/**
 * 订单服务端口
 */
public interface IOrderServicePort {

    String queryOrderIdByOutTradeNo(String userId, String outTradeNo);

    /**
     * 执行退款
     */
    void refundExecute(String userId, String orderId);

}
