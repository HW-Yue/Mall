package com.yue.seckill.api.dto;

import lombok.Data;

@Data
public class CreateSeckillOrderRequestDTO {

    private String userId;
    private String productId;
    private Long activityId;
    private String source;
    private String channel;
    /** 商品名称（传给 order-service 冗余存储） */
    private String goodsName;
    /** 商品图片URL（传给 order-service 冗余存储） */
    private String goodsImageUrl;
    /**
     * 压测模式：true 时跳过 Redis 库存扣减和 MQ 发送，直接返回 mock token。
     * 仅用于接口压测，生产环境不传或传 false。
     */
    private Boolean isTest;
}
