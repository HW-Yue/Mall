package com.yue.order.domain.order.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusVO {

    LOCK(0, "LOCK", "锁单 - 订单已创建，等待支付"),
    PAY_SUCCESS(1, "PAY_SUCCESS", "支付成功"),
    CLOSE(2, "CLOSE", "关闭 - 超时未支付或退款完成"),
    ;

    private final int dbValue;
    private final String code;
    private final String desc;

    public static OrderStatusVO fromDbValue(Integer dbValue) {
        if (dbValue == null) return LOCK;
        for (OrderStatusVO v : values()) {
            if (v.dbValue == dbValue) return v;
        }
        return LOCK;
    }
}
