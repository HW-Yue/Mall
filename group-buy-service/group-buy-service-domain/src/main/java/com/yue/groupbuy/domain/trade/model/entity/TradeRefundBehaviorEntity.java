package com.yue.groupbuy.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeRefundBehaviorEntity {

    /** 用户ID */
    private String userId;
    /** 订单ID */
    private String orderId;
    /** 组队ID */
    private String teamId;
    /** 退单行为枚举 */
    private TradeRefundBehaviorEnum tradeRefundBehaviorEnum;

    @AllArgsConstructor
    public enum TradeRefundBehaviorEnum {
        SUCCESS("success", "退单成功"),
        REPEAT("repeat", "重复退单"),
        FAIL("fail", "退单失败"),
        ;
        private final String code;
        private final String info;

        public String getCode() { return code; }
        public String getInfo() { return info; }
    }

}
