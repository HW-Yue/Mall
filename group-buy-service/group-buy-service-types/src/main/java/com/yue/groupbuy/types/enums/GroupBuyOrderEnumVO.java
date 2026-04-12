package com.yue.groupbuy.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GroupBuyOrderEnumVO {

    PROGRESS(0, "拼单中"),
    COMPLETE(1, "完成"),
    FAIL(2, "失败"),
    COMPLETE_FAIL(3, "完成失败"),
    ;

    private final Integer code;
    private final String info;

    public static GroupBuyOrderEnumVO valueOf(Integer code) {
        for (GroupBuyOrderEnumVO value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }

}
