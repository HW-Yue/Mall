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
public class TOrderGroup {

    private String orderId;
    private String userId;
    private String teamId;
    private Long activityId;
    private String goodsId;
    /** 0=锁定 1=已支付 2=退款处理中 3=已关团（超时未支付） 4=已退款 */
    private Integer status;
    private BigDecimal originalPrice;
    private BigDecimal deductionPrice;
    private BigDecimal payPrice;
    private Date outTradeTime;
    private Date startTime;
    private Date endTime;
    private Date createTime;
    private Date updateTime;
}
