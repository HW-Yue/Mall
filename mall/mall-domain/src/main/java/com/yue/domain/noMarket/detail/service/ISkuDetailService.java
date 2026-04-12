package com.yue.domain.noMarket.detail.service;

import com.yue.domain.noMarket.detail.model.entity.SkuDetailEntity;

/**
 * 普通商品详情服务接口
 */
public interface ISkuDetailService {

    /**
     * 查询普通商品详情
     *
     * @param goodsId 商品ID
     * @return 商品详情
     */
    SkuDetailEntity querySkuDetail(String goodsId);

}

