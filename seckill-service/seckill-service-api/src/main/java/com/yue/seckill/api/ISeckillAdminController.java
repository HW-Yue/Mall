package com.yue.seckill.api;

import com.yue.seckill.api.dto.PreheatRequestDTO;
import com.yue.seckill.api.dto.PreheatResponseDTO;
import com.yue.seckill.api.dto.QueryActivitiesResponseDTO;
import com.yue.seckill.api.response.Response;

/**
 * 秒杀管理后台接口（预热管理）
 */
public interface ISeckillAdminController {

    /**
     * 查询有效活动及商品列表（含当前预热状态）
     */
    Response<QueryActivitiesResponseDTO> queryActivities();

    /**
     * 手动预热指定活动/商品的库存到 Redis
     */
    Response<PreheatResponseDTO> preheat(PreheatRequestDTO request);

}
