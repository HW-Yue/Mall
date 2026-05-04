package com.yue.groupbuy.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TradeOrderStatusEnumVO {

    /** 0 - 锁定，订单已建未支付 */
    CREATE(0, "锁定（未支付）"),
    /** 1 - 已支付，结算成功后由 settlementMarketPayOrder 翻 0→1 */
    COMPLETE(1, "已支付"),
    /** 2 - 退款处理中，已支付订单触发退款后 update2Refund 翻 1→2 */
    WAIT_REFUND(2, "退款处理中"),
    /** 3 - 已关团，未支付订单被关单后 closeUnpaid* 翻 0→3（系统超时 / 用户主动取消 / pay 关单 共用） */
    CLOSED(3, "已关团"),
    /** 4 - 已退款，pay-refund-*-result 回执后 update2Refunded 翻 2→4 */
    REFUNDED(4, "已退款"),
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
