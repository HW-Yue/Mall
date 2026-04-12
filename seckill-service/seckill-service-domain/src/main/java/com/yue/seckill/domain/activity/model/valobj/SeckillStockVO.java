package com.yue.seckill.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀库存预热对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillStockVO {

    private Long activityId;
    private String goodsId;
    private Integer remainCount;

}
