package com.yue.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 创建订单请求 DTO
 * 普通下单：前端直接调用；拼团/秒杀：各自营销服务调用
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private String userId;
    /** 商品ID */
    private String productId;
    /** 营销类型：normal / group_buy / seckill */
    private String marketType;
    /** 原始价格 */
    private BigDecimal originalPrice;
    /** 折扣金额（普通商品为0） */
    private BigDecimal deductionPrice;
    /** 支付金额 */
    private BigDecimal payPrice;
    /** 来源 */
    private String source;
    /** 渠道 */
    private String channel;
    /** 商品名称（由调用方传入，下单时冗余写入，查询订单直接返回，无需跨服务查询） */
    private String goodsName;
    /** 商品图片URL（由调用方传入，冗余存储） */
    private String goodsImageUrl;
}
