package com.yue.seckill.api;

import com.yue.seckill.api.dto.QuerySeckillGoodsListResponseDTO;
import com.yue.seckill.api.response.Response;

/**
 * 秒杀市场接口
 */
public interface ISeckillMarketController {

    /**
     * 查询秒杀商品列表
     */
    Response<QuerySeckillGoodsListResponseDTO> querySeckillGoodsList();

}
