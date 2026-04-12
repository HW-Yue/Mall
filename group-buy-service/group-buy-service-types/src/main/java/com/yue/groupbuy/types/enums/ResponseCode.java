package com.yue.groupbuy.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCode {

    SUCCESS("0000", "成功"),
    UN_ERROR("0001", "未知失败"),
    ILLEGAL_PARAMETER("0002", "非法参数"),
    E0001("E0001", "不存在对应的折扣计算服务"),
    E0002("E0002", "商品无对应的营销活动配置"),
    E0003("E0003", "拼团活动服务降级"),
    E0004("E0004", "用户不在切量范围内"),
    E0005("E0005", "拼团组队失败，记录更新为0"),
    E0101("E0101", "拼团活动非生效状态"),
    E0102("E0102", "拼团活动非可参与时间范围"),
    E0103("E0103", "超过参团次数限制"),
    E0104("E0104", "支付订单不存在或已关闭"),
    E0105("E0105", "SC渠道黑名单拦截"),
    E0106("E0106", "结算时间超出拼团有效期"),
    E0008("E0008", "拼团库存不足"),
    ACTIVITY_NOT_FOUND("1001", "活动不存在或未生效"),
    TEAM_FULL("1002", "拼团已满"),
    TAKE_LIMIT_EXCEEDED("1003", "超过参团次数限制"),
    ORDER_NOT_FOUND("1004", "订单不存在"),
    ORDER_STATUS_ERROR("1005", "订单状态错误"),
    REFUND_NOT_ALLOWED("1006", "当前状态不允许退款"),
    CREATE_ORDER_FAILED("1007", "创建订单失败"),
    INDEX_EXCEPTION("9999", "数据冲突，请重试"),
    UPDATE_ZERO("9998", "更新记录为零");

    private final String code;
    private final String info;
}
