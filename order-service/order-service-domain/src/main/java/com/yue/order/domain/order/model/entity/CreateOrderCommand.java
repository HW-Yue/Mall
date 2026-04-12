package com.yue.order.domain.order.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 创建订单命令（domain 层输入）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderCommand {

    private String userId;
    private String goodsId;
    private String marketType;
    private BigDecimal originalPrice;
    private BigDecimal deductionPrice;
    private BigDecimal payPrice;
    private String source;
    private String channel;
    /** 外部交易单号（可选，不传则自动生成） */
    private String outTradeNo;
    /** 商品名称（冗余写入 t_order，查询订单直接返回） */
    private String goodsName;
    /** 商品图片URL（冗余写入 t_order） */
    private String goodsImageUrl;
}
