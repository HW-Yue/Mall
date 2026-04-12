package com.yue.seckill.infrastructure.dao.po;

import lombok.Data;

/**
 * 折扣配置
 */
@Data
public class Discount {

    private Long id;
    private String discountId;
    private String discountName;
    private String discountDesc;
    private Integer discountType;
    private String marketPlan;
    private String marketExpr;
    private String tagId;

}
