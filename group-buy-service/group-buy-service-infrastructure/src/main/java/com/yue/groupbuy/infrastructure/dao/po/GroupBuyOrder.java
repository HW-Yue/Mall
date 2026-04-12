package com.yue.groupbuy.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyOrder {

    private Long id;
    private String teamId;
    private Long activityId;
    private String source;
    private String channel;
    private BigDecimal originalPrice;
    private BigDecimal deductionPrice;
    private BigDecimal payPrice;
    private Integer targetCount;
    private Integer completeCount;
    private Integer lockCount;
    /** 0-拼单中、1-完成、2-失败、3-部分退款 */
    private Integer status;
    private Date validStartTime;
    private Date validEndTime;
    private String notifyType;
    private String notifyUrl;
    private Date createTime;
    private Date updateTime;
}
