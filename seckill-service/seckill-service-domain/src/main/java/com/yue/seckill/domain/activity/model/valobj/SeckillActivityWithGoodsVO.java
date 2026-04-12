package com.yue.seckill.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 秒杀活动（含商品列表）值对象，用于管理后台预热选择
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillActivityWithGoodsVO {

    private Long activityId;
    private String activityName;
    private Integer remainCount;
    private List<GoodsItem> goodsList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoodsItem {
        private String goodsId;
        private String goodsName;
    }

}
