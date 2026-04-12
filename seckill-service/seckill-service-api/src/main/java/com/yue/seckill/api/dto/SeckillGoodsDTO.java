package com.yue.seckill.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 秒杀商品 DTO
 */
@Data
@Builder
public class SeckillGoodsDTO {

    private String goodsId;
    private String goodsName;
    private String goodsImageUrl;
    private BigDecimal originalPrice;
    private BigDecimal payPrice;
    private String source;
    private String channel;
    private Long activityId;

}
