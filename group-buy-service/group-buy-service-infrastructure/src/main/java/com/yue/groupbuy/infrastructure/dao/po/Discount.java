package com.yue.groupbuy.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Discount {

    private String discountId;
    private String discountName;
    private String discountDesc;
    /** 0:base、1:tag */
    private Integer discountType;
    /** ZJ-直减、MJ-满减、ZK-折扣、N-N元购 */
    private String marketPlan;
    private String marketExpr;
    /** 人群标签ID */
    private String tagId;
}
