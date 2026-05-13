package com.yue.groupbuy.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradePaySettlementEntity {

    /** 来源 */
    private String source;
    /** 渠道 */
    private String channel;
    /** 用户ID */
    private String userId;
    /** 组队ID */
    private String teamId;
    /** 活动ID */
    private Long activityId;
    /** 订单ID */
    private String orderId;

}
