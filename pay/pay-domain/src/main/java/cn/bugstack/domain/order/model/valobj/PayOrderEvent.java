package cn.bugstack.domain.order.model.valobj;

/**
 * 支付订单状态机触发事件。配合 {@link PayOrderStateMachine} 校验合法转移。
 */
public enum PayOrderEvent {

    /** 创建后落支付单 */
    CREATE_DONE,
    /** 支付宝回调成功 */
    PAY_SUCCESS,
    /** 超时关单 */
    CLOSE_TIMEOUT,
    /** 关单后到账（异常补偿）*/
    LATE_PAYMENT,
    /** 营销结算完成 */
    MARKETING_DONE,
    /** 商品发货完成 */
    DEAL_DONE,
    /** 触发退款 */
    REFUND_REQUEST,
    /** 退款完成 */
    REFUND_COMPLETE,

}
