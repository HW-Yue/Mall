package com.yue.order.domain.order.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusVO {

    LOCK(0, "LOCK", "锁单 - 订单已创建，等待支付"),
    PAY_SUCCESS(1, "PAY_SUCCESS", "支付成功"),
    CLOSE(2, "CLOSE", "关闭 - 超时未支付"),
    WAIT_REFUND(3, "WAIT_REFUND", "待退款 - 已触发退款，等待支付侧完成"),
    REFUNDED(4, "REFUNDED", "已退款 - 支付侧退款完成"),
    WAIT_SHIP(5, "WAIT_SHIP", "待发货 - 拼团成功，进入履约阶段"),
    SHIPPED(6, "SHIPPED", "已发货 - 发货任务已执行"),
    DELIVERED(7, "DELIVERED", "已签收 - 预留状态，待物流回调接入"),
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
