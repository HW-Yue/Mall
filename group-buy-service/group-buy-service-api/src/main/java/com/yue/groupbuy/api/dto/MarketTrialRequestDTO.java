package com.yue.groupbuy.api.dto;

import lombok.Data;

@Data
public class MarketTrialRequestDTO {

    /** 用户ID */
    private String userId;
    /** 商品ID */
    private String goodsId;
    /** 活动ID（可为空，为空时通过 source+channel+goodsId 从 sc_sku_activity 查找） */
    private Long activityId;
    /** 渠道 */
    private String source;
    /** 来源 */
    private String channel;

}
