package com.yue.infrastructure.adapter.repository;

import com.yue.domain.noMarket.detail.adapter.repository.ISkuDetailRepository;
import com.yue.domain.noMarket.detail.model.entity.SkuDetailEntity;
import com.yue.infrastructure.dao.ISkuDao;
import com.yue.infrastructure.dao.po.Sku;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

/**
 * 普通商品详情仓储实现
 */
@Repository
public class SkuDetailRepository implements ISkuDetailRepository {

    @Resource
    private ISkuDao skuDao;

    @Override
    public SkuDetailEntity querySkuDetail(String goodsId) {
        Sku sku = skuDao.querySkuByGoodsId(goodsId);
        if (sku == null) {
            return null;
        }
        return SkuDetailEntity.builder()
                .id(sku.getGoodsId())
                .imageUrl(sku.getGoodsImageUrl())
                .name(sku.getGoodsName())
                .price(sku.getOriginalPrice())
                .goodsDetail(sku.getGoodsDetail())
                .build();
    }

}

