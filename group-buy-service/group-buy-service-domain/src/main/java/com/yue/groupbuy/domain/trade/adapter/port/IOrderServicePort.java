package com.yue.groupbuy.domain.trade.adapter.port;

import java.math.BigDecimal;
import java.util.Date;

public interface IOrderServicePort {

    /**
     * 调用 order-service 创建订单，返回 orderId；同时本地插入 t_order_group 关联记录
     */
    String createOrder(String userId, String productId, String goodsName, String goodsImageUrl,
                       BigDecimal originalPrice, BigDecimal deductionPrice, BigDecimal payPrice,
                       String source, String channel,
                       String teamId, Long activityId, Date startTime, Date endTime);

    /**
     * 调用 order-service 执行退款（已验证业务规则后调用）
     */
    void refundExecute(String userId, String orderId);
}
