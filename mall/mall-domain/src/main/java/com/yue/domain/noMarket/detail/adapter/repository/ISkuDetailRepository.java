package com.yue.domain.noMarket.detail.adapter.repository;

import com.yue.domain.noMarket.detail.model.entity.SkuDetailEntity;

/**
 * 普通商品详情仓储接口
 */
public interface ISkuDetailRepository {

    /**
     * 根据商品ID查询商品详情
     *
     * @param goodsId 商品ID
     * @return 商品详情
     */
    SkuDetailEntity querySkuDetail(String goodsId);

}

