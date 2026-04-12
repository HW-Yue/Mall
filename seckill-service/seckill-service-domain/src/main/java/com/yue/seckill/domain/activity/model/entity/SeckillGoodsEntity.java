package com.yue.seckill.domain.activity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 秒杀商品实体（供前端展示）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillGoodsEntity {

    private String goodsId;
    private String goodsName;
    private String goodsImageUrl;
    private BigDecimal originalPrice;
    private BigDecimal payPrice;
    private String source;
    private String channel;
    private Long activityId;

}
