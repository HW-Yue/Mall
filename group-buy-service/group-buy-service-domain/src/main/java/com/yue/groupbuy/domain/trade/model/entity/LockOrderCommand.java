package com.yue.groupbuy.domain.trade.model.entity;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 拼团锁单命令（domain service 入参）
 */
@Data
@Builder
public class LockOrderCommand {

    private String userId;
    private String productId;
    private String teamId;
    private Long activityId;
    private String source;
    private String channel;

    // 试算结果（由 domain service 填充）
    private String activityName;
    private Integer targetCount;
    private Integer validTime;
    private BigDecimal originalPrice;
    private BigDecimal deductionPrice;
    private BigDecimal payPrice;
}
