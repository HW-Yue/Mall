package com.yue.groupbuy.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradePaySuccessEntity {

    /** 来源 */
    private String source;
    /** 渠道 */
    private String channel;
    /** 用户ID */
    private String userId;
    /** 订单ID */
    private String orderId;
    /** 支付时间 */
    private Date outTradeTime;

}
