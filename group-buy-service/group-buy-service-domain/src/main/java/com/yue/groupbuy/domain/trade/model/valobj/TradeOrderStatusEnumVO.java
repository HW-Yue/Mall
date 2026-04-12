package com.yue.groupbuy.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TradeOrderStatusEnumVO {

    CREATE(0, "创建"),
    COMPLETE(1, "完成"),
    CLOSE(2, "关闭"),
    ;

    private final Integer code;
    private final String info;

    public static TradeOrderStatusEnumVO valueOf(Integer code) {
        for (TradeOrderStatusEnumVO value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }

}
