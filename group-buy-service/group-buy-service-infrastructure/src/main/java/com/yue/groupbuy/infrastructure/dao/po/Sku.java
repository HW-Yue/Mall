package com.yue.groupbuy.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Sku {

    private String goodsId;
    private String goodsName;
    private String goodsImageUrl;
    private String goodsDetail;
    private BigDecimal originalPrice;
    private Integer categoryId;
}
