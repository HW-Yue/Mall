package com.yue.seckill.domain.activity.service;

import com.yue.seckill.domain.activity.model.entity.SeckillGoodsEntity;

import java.util.List;

/**
 * 秒杀市场服务接口
 */
public interface ISeckillMarketService {

    /**
     * 查询秒杀商品列表
     */
    List<SeckillGoodsEntity> querySeckillGoodsList();

}
