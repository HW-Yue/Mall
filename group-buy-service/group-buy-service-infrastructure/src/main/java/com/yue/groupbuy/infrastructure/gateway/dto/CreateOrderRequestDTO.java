package com.yue.groupbuy.infrastructure.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CreateOrderRequestDTO {

    private String userId;
    private String productId;
    private String marketType;
    private BigDecimal originalPrice;
    private BigDecimal deductionPrice;
    private BigDecimal payPrice;
    private String source;
    private String channel;
    private String outTradeNo;
    /** 商品名称（冗余存储，查询订单时直接返回） */
    private String goodsName;
    /** 商品图片URL（冗余存储） */
    private String goodsImageUrl;
}
