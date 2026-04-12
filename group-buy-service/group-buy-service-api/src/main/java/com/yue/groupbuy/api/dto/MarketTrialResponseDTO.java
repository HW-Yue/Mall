package com.yue.groupbuy.api.dto;

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
public class MarketTrialResponseDTO {

    /** 商品ID */
    private String goodsId;
    /** 商品名称 */
    private String goodsName;
    /** 商品图片URL */
    private String goodsImageUrl;
    /** 商品详情介绍 */
    private String goodsDetail;
    /** 原始价格 */
    private BigDecimal originalPrice;
    /** 折扣金额 */
    private BigDecimal deductionPrice;
    /** 支付金额 */
    private BigDecimal payPrice;
    /** 活动ID */
    private Long activityId;
    /** 活动名称 */
    private String activityName;
    /** 折扣描述 */
    private String discountDesc;
    /** 成团目标人数 */
    private Integer targetCount;
    /** 活动开始时间 */
    private Date startTime;
    /** 活动结束时间 */
    private Date endTime;
    /** 是否可见拼团入口 */
    private Boolean isVisible;
    /** 是否可参与拼团 */
    private Boolean isEnable;

}
