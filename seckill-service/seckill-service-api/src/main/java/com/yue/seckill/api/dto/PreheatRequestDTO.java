package com.yue.seckill.api.dto;

import lombok.Data;

/**
 * 管理后台手动预热库存请求 DTO
 */
@Data
public class PreheatRequestDTO {

    /** 活动ID */
    private Long activityId;

    /**
     * 商品ID，传 "all" 表示预热该活动下所有商品
     */
    private String goodsId;

    /**
     * 预热库存数量，不传则使用数据库中的 remainCount
     */
    private Integer stock;

    /**
     * 过期时间（秒），默认 7200（2小时）
     */
    private Long expireSeconds;

}
