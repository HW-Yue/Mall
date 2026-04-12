package com.yue.groupbuy.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamRefundSuccess {

    /** 退单类型 */
    private String type;
    /** 用户ID */
    private String userId;
    /** 组队ID */
    private String teamId;
    /** 活动ID */
    private Long activityId;
    /** 订单ID */
    private String orderId;

}
