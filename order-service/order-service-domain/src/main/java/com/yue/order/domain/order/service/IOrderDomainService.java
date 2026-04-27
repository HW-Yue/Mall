package com.yue.order.domain.order.service;

import com.yue.order.domain.order.model.entity.CreateOrderCommand;
import com.yue.order.domain.order.model.entity.NormalOrderEnqueueResult;
import com.yue.order.domain.order.model.entity.OrderEntity;

import java.util.List;

/**
 * 订单领域服务接口
 */
public interface IOrderDomainService {

    /**
     * 创建订单（锁单），返回 orderId
     */
    String createOrder(CreateOrderCommand command);

    /**
     * 按 userId + outTradeNo 查询已创建订单，用于服务间超时确认
     */
    OrderEntity queryByUserIdAndOutTradeNo(String userId, String outTradeNo);

    /**
     * 普通商品：mall 已锁可售库存，生成本服务 orderId/outTradeNo，同步发 MQ 异步落单，不经过 mall 二次锁库
     */
    NormalOrderEnqueueResult submitNormalOrderFromMall(CreateOrderCommand command);

    /**
     * 获取/创建支付 URL（用户点击支付时调用）
     */
    String getPayUrl(String userId, String orderId);

    /**
     * 处理支付成功回调（由 MQ 消费者调用）
     *
     * @param outTradeNo  外部交易单号
     * @param marketType  营销类型
     * @param outTradeTime 支付时间
     */
    void handlePaySuccess(String outTradeNo, String marketType, java.util.Date outTradeTime);

    /**
     * 普通订单退款（前端调用，含业务状态校验）
     */
    void refund(String userId, String orderId);

    /**
     * 营销订单退款执行（营销服务调用，跳过业务校验）
     */
    void refundExecute(String userId, String orderId);

    /**
     * 查询用户订单列表（游标分页），返回 count 条
     */
    List<OrderEntity> queryUserOrderList(String userId, Long lastId, int count);

    /**
     * 处理订单关单通知（由 MQ 消费者调用）
     *
     * @param outTradeNo 外部交易单号
     */
    void handleOrderClose(String outTradeNo);

    /**
     * 处理关单后退款通知（由 MQ 消费者调用）
     *
     * @param outTradeNo 外部交易单号
     */
    void handlePayRefund(String outTradeNo);

    /**
     * 触发超时未支付订单关单
     */
    void triggerTimeoutClose();
}
