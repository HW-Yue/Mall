package com.yue.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 按活动类型分组的活动商品返回对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityGoodsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 拼团活动商品列表 */
    private List<ActivityGoodsItem> groupBuyList;

    /** 秒杀活动商品列表 */
    private List<ActivityGoodsItem> seckillList;

    /**
     * 单个活动商品
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityGoodsItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 活动ID */
        private Long activityId;
        /** 活动类型编码，如：group_buy、seckill */
        private String activityType;
        /** 商品ID */
        private String goodsId;
        /** 商品名称 */
        private String goodsName;
        /** 商品图片URL */
        private String imageUrl;
        /** 商品原价（列表展示可作为划线价） */
        private BigDecimal originalPrice;
        /** 活动类型计划；ZJ:直减、N: N元购 */
        private String marketPlan;
        /** 折扣表达式原值；ZJ 为直减金额，N 为最终购入价 */
        private BigDecimal discountValue;
        /** 应用折扣后的活动价（首页展示价） */
        private BigDecimal activityPrice;
        /** 渠道 source */
        private String source;
        /** 来源 channel */
        private String channel;
    }
}

