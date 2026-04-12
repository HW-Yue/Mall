package com.yue.order.domain.order.adapter.port;

/**
 * 支付网关端口（出站）
 */
public interface IPayPort {

    /**
     * 调用支付服务创建支付单，获取 Alipay 支付链接
     *
     * @param userId        用户ID
     * @param outTradeNo    外部交易单号
     * @param goodsId       商品ID
     * @param goodsName     商品名称
     * @param originalPrice 原始价格
     * @param deductionPrice 折扣金额
     * @param payPrice      支付金额
     * @param marketType    营销类型
     * @return Alipay 支付链接
     */
    String getPayUrl(String userId, String outTradeNo, String goodsId, String goodsName,
                     java.math.BigDecimal originalPrice, java.math.BigDecimal deductionPrice,
                     java.math.BigDecimal payPrice, String marketType);

    /**
     * 调用支付服务发起退款
     *
     * @param outTradeNo 外部交易单号
     * @param payPrice   退款金额
     * @param reason     退款原因
     */
    void refund(String outTradeNo, java.math.BigDecimal payPrice, String reason);
}
