package com.yue.seckill.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCode {

    SUCCESS("0000", "成功"),
    UN_ERROR("0001", "未知失败"),
    ILLEGAL_PARAMETER("0002", "非法参数"),
    STOCK_INSUFFICIENT("1001", "秒杀库存不足"),
    ORDER_NOT_FOUND("1002", "订单不存在"),
    CREATE_ORDER_FAILED("1003", "创建订单失败"),
    ACTIVITY_NOT_FOUND("1004", "活动不存在"),
    ACTIVITY_NOT_EFFECTIVE("1005", "活动未生效"),
    USER_TAKE_LIMIT("1006", "用户参与次数已达上限"),
    STOCK_NOT_ENOUGH("1007", "库存不足"),
    REPEATED_REQUEST("1008", "重复请求，已参与过秒杀");

    private final String code;
    private final String info;
}
