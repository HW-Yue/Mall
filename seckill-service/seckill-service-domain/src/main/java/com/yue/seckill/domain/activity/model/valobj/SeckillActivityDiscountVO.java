package com.yue.seckill.domain.activity.model.valobj;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 秒杀活动折扣值对象
 */
@Data
@Builder
public class SeckillActivityDiscountVO {

    private Long activityId;
    private String activityName;
    private SeckillDiscount seckillDiscount;
    private Integer stockCount;
    private Integer remainCount;
    private Integer takeLimitCount;
    private Integer status;
    private Date startTime;
    private Date endTime;
    private String tagId;
    private String tagScope;

    @Data
    @Builder
    public static class SeckillDiscount {
        private String discountName;
        private String discountDesc;
        private Integer discountType;
        private String marketPlan;
        private String marketExpr;
        private String tagId;
    }

}
