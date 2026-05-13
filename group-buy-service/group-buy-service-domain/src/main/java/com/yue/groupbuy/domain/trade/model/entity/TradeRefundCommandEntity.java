package com.yue.groupbuy.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeRefundCommandEntity {

    /** 用户ID */
    private String userId;
    /** 订单ID */
    private String orderId;
    /** 来源 */
    private String source;
    /** 渠道 */
    private String channel;

}
