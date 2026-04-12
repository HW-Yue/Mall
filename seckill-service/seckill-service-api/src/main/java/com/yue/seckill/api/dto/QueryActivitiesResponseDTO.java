package com.yue.seckill.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 查询活动（含商品）响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryActivitiesResponseDTO {

    private List<ActivityWithGoodsDTO> activities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityWithGoodsDTO {
        private Long activityId;
        private String activityName;
        private Integer remainCount;
        private List<GoodsItemDTO> goodsList;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoodsItemDTO {
        private String goodsId;
        private String goodsName;
        /** Redis 中当前库存值，null 表示未预热 */
        private String currentStock;
    }

}
