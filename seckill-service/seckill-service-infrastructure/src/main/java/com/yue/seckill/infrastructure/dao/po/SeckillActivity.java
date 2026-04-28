package com.yue.seckill.infrastructure.dao.po;

import lombok.Data;

import java.util.Date;
import java.math.BigDecimal;

/**
 * 秒杀活动
 */
@Data
public class SeckillActivity {

    private Long id;
    private Long activityId;
    private String activityName;
    private BigDecimal seckillPrice;
    private Integer takeLimitCount;
    private Integer status;
    private Date startTime;
    private Date endTime;
    private String tagId;
    private String tagScope;

}
