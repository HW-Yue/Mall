package com.yue.groupbuy.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 拼团商品 DTO
 */
@Data
@Builder
public class GroupBuyGoodsDTO {

    private String goodsId;
    private String goodsName;
    private String goodsImageUrl;
    private BigDecimal originalPrice;
    private BigDecimal payPrice;
    private String source;
    private String channel;
    private Long activityId;

}
