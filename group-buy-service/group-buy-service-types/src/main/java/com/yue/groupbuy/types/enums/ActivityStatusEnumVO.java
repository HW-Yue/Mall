package com.yue.groupbuy.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ActivityStatusEnumVO {

    CREATE(0, "创建"),
    EFFECTIVE(1, "生效"),
    OVERDUE(2, "过期"),
    ABANDONED(3, "废弃"),
    ;

    private final Integer code;
    private final String info;

    public static ActivityStatusEnumVO valueOf(Integer code) {
        for (ActivityStatusEnumVO value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }

}
