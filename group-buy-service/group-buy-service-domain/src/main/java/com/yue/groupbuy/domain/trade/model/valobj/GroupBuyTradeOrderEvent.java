package com.yue.groupbuy.domain.trade.model.valobj;

/**
 * 拼团个人订单（group_buy_service.t_order）状态机触发事件。
 * 配合 {@link GroupBuyTradeOrderStateMachine}。
 */
public enum GroupBuyTradeOrderEvent {

    /** 支付成功（settlementMarketPayOrder） */
    PAID,
    /** 关单（系统超时 / 用户主动取消 共用，CREATE→CLOSED） */
    CLOSE,
    /** 触发退款（COMPLETE→WAIT_REFUND） */
    REFUND_REQUEST,
    /** 退款回执完成（WAIT_REFUND→REFUNDED） */
    REFUND_COMPLETE,

}
