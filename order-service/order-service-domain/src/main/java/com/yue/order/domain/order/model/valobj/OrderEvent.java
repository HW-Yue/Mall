package com.yue.order.domain.order.model.valobj;

/**
 * 订单状态机触发事件。配合 {@link OrderStateMachine} 校验合法转移。
 */
public enum OrderEvent {

    /** 支付成功 */
    PAID,
    /** 关单（系统超时 / 用户主动取消 共用） */
    CLOSE,
    /** 触发退款 */
    REFUND_REQUEST,
    /** 退款回执完成 */
    REFUND_COMPLETE,
    /** 进入待发货 */
    SHIP,
    /** 发货完成 */
    SHIP_DONE,
    /** 物流签收 */
    DELIVER,

}
