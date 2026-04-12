package com.yue.groupbuy.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GroupBuyOrderStatusVO {

    PROGRESS(0, "拼单中"),
    COMPLETE(1, "完成"),
    FAIL(2, "失败"),
    PARTIAL_REFUND(3, "部分退款");

    private final int code;
    private final String desc;

    public static GroupBuyOrderStatusVO valueOf(int code) {
        for (GroupBuyOrderStatusVO v : values()) {
            if (v.code == code) return v;
        }
        throw new IllegalArgumentException("Unknown GroupBuyOrderStatusVO code: " + code);
    }
}
