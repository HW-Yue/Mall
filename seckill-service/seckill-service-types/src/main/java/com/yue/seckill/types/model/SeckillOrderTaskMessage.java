package com.yue.seckill.types.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 秒杀下单任务消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderTaskMessage {

    private String seckillToken;
    private String userId;
    private String productId;
    private Long activityId;
    private String source;
    private String channel;
    private String goodsName;
    private String goodsImageUrl;
    private String outTradeNo;
    private BigDecimal originalPrice;
    private BigDecimal deductionPrice;
    private BigDecimal payPrice;

}
