package com.yue.seckill.domain.activity.model.valobj;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品值对象
 */
@Data
@Builder
public class SkuVO {

    private String goodsId;
    private String goodsName;
    private String goodsImageUrl;
    private String goodsDetail;
    private BigDecimal originalPrice;

}
