package com.yue.order.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ResponseCode {

    SUCCESS("0000", "成功"),
    UN_ERROR("0001", "未知失败"),
    ILLEGAL_PARAMETER("0002", "非法参数"),
    INDEX_EXCEPTION("0003", "唯一索引冲突"),
    UPDATE_ZERO("0004", "更新记录为0"),
    HTTP_EXCEPTION("0005", "HTTP接口调用异常"),
    ORDER_NOT_FOUND("1001", "订单不存在"),
    ORDER_STATUS_ERROR("1002", "订单状态不允许此操作"),
    STOCK_INSUFFICIENT("1003", "可售库存不足"),
    PAY_URL_FAILED("1004", "获取支付链接失败"),
    REFUND_FAILED("1005", "退款失败"),
    ;

    private String code;
    private String info;
}
