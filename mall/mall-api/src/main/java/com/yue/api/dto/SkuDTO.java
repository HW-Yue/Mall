package com.yue.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SkuDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String goodsId;
    private String goodsName;
    private String goodsImageUrl;
    private BigDecimal originalPrice;
    private Integer categoryId;
    private Integer totalStock;
    private Integer lockedStock;
}
