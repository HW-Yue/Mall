package com.yue.groupbuy.domain.trade.model.entity;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 活动定价实体（试算结果）
 */
@Data
@Builder
public class ActivityPricingEntity {

    private Long activityId;
    private String activityName;
    private Integer target;
    private Integer validTime;
    private Integer takeLimitCount;
    private Date startTime;
    private Date endTime;

    private BigDecimal originalPrice;
    private BigDecimal deductionPrice;
    private BigDecimal payPrice;
}
