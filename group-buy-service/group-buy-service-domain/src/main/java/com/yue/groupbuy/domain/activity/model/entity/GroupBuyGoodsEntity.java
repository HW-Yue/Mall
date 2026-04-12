package com.yue.groupbuy.domain.activity.model.entity;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 拼团商品实体
 */
@Data
@Builder
public class GroupBuyGoodsEntity {

    private String goodsId;
    private String goodsName;
    private String goodsImageUrl;
    private BigDecimal originalPrice;
    private BigDecimal payPrice;
    private String source;
    private String channel;
    private Long activityId;

}
