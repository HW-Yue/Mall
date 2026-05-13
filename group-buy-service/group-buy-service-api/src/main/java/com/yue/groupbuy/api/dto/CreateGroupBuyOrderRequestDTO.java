package com.yue.groupbuy.api.dto;

import lombok.Data;

@Data
public class CreateGroupBuyOrderRequestDTO {

    /** 用户ID */
    private String userId;
    /** 商品ID */
    private String productId;
    /** 队伍ID（加入已有队伍时传入，新建队伍则为 null） */
    private String teamId;
    /** 活动ID */
    private Long activityId;
    /** 来源 */
    private String source;
    /** 渠道 */
    private String channel;
}
