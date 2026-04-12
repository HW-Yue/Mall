package com.yue.seckill.domain.activity.model.entity;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 秒杀活动实体
 */
@Data
@Builder
public class SeckillActivityEntity {

    private Long activityId;
    private String activityName;
    private String discountId;
    private Integer stockCount;
    private Integer remainCount;
    private Integer takeLimitCount;
    private Integer status;
    private Date startTime;
    private Date endTime;
    private String tagId;
    private String tagScope;

}
