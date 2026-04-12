package com.yue.seckill.domain.trade.adapter.port;

import java.math.BigDecimal;

/**
 * 订单服务端口
 */
public interface IOrderServicePort {

    /**
     * 创建订单
     */
    String createOrder(String userId, String productId, String goodsName, String goodsImageUrl,
                       BigDecimal originalPrice, BigDecimal deductionPrice, BigDecimal payPrice,
                       String source, String channel, String outTradeNo);

    /**
     * 执行退款
     */
    void refundExecute(String userId, String orderId);

}
