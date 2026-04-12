package com.yue.domain.noMarket.detail.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 普通商品详情领域实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuDetailEntity {

    /** 商品ID */
    private String id;
    /** 商品图片URL */
    private String imageUrl;
    /** 商品名称 */
    private String name;
    /** 价格 */
    private BigDecimal price;
    /** 渠道 source */
    private String source;
    /** 来源 channel */
    private String channel;
    /** 商品详情介绍 */
    private String goodsDetail;

}

