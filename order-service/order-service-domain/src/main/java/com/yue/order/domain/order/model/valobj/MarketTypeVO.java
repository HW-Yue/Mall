package com.yue.order.domain.order.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MarketTypeVO {

    NORMAL("normal", "普通商品"),
    GROUP_BUY("group_buy", "拼团业务"),
    SECKILL("seckill", "秒杀业务"),
    ;

    private final String code;
    private final String desc;

    public static MarketTypeVO fromCode(String code) {
        if (code == null) return NORMAL;
        for (MarketTypeVO v : values()) {
            if (v.code.equals(code)) return v;
        }
        throw new IllegalArgumentException("Unknown marketType: " + code);
    }
}
