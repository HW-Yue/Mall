package com.yue.seckill.infrastructure.dao.po;

import lombok.Data;

/**
 * 渠道-商品-活动映射（秒杀）
 */
@Data
public class ScSkuActivity {

    private Integer id;
    private String source;
    private String channel;
    private Long activityId;
    private String activityType;
    private String goodsId;

}
