package com.yue.groupbuy.api.dto;

import lombok.Data;

@Data
public class QueryGroupBuyOrderDetailRequestDTO {

    /** 活动ID */
    private Long activityId;
    /** 用户ID */
    private String userId;
    /** 展示当前用户参团记录数量 */
    private Integer ownerCount;
    /** 展示随机参团记录数量 */
    private Integer randomCount;

}
