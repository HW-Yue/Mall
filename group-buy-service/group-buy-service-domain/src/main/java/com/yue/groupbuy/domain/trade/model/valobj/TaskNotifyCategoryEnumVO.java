package com.yue.groupbuy.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskNotifyCategoryEnumVO {

    TRADE_SETTLEMENT("trade_settlement", "交易结算"),
    TRADE_UNPAID2REFUND("trade_unpaid2refund", "未支付退款"),
    TRADE_PAID2REFUND("trade_paid2refund", "已付未成团退款"),
    TRADE_PAID_TEAM2REFUND("trade_paid_team2refund", "已付已成团退款"),
    ;

    private final String code;
    private final String info;

    public static TaskNotifyCategoryEnumVO getByCode(String code) {
        for (TaskNotifyCategoryEnumVO value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }

}
