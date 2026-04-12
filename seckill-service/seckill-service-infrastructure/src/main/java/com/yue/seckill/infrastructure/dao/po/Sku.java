package com.yue.seckill.infrastructure.dao.po;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品信息
 */
@Data
public class Sku {

    private Integer id;
    private String goodsId;
    private String goodsName;
    private String goodsImageUrl;
    private String goodsDetail;
    private BigDecimal originalPrice;
    private Integer categoryId;

}
