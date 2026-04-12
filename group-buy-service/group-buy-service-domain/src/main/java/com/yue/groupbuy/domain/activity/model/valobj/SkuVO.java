package com.yue.groupbuy.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkuVO {

    /** 商品ID */
    private String goodsId;
    /** 商品名称 */
    private String goodsName;
    /** 商品图片URL */
    private String goodsImageUrl;
    /** 商品详情介绍 */
    private String goodsDetail;
    /** 原始价格 */
    private BigDecimal originalPrice;

}
