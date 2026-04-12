package com.yue.order.domain.order.adapter.repository;

import com.yue.order.domain.order.model.entity.OrderEntity;

import java.util.Date;
import java.util.List;

/**
 * 订单仓储接口（出站）
 */
public interface IOrderRepository {

    /**
     * 保存订单记录（INSERT t_order, status=0）
     * 对普通商品同时锁 sku 库存；拼团/秒杀由调用方保证
     */
    String saveOrder(OrderEntity orderEntity);

    /** 按用户ID + 订单ID 查询 */
    OrderEntity queryByUserIdAndOrderId(String userId, String orderId);

    /** 按外部交易单号查询 */
    OrderEntity queryByOutTradeNo(String outTradeNo);

    /** 更新 pay_url */
    void updatePayUrl(String userId, String outTradeNo, String payUrl);

    /** 支付成功：更新 status=1, out_trade_time */
    void updatePaySuccess(String outTradeNo, Date outTradeTime);

    /** 标记退款：更新 status=2 */
    void updateRefund(String userId, String orderId);

    /** 查询用户订单列表（游标分页，返回 count 条） */
    List<OrderEntity> queryUserOrderList(String userId, Long lastId, int count);
}
