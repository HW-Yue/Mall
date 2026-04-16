package com.yue.domain.noMarket.detail.service;

import com.yue.domain.noMarket.detail.adapter.repository.ISkuDetailRepository;
import com.yue.domain.noMarket.detail.model.entity.SkuDetailEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 普通商品详情服务
 */
@Slf4j
@Service
public class SkuDetailService implements ISkuDetailService {

    @Resource
    private ISkuDetailRepository repository;

    @Override
    public SkuDetailEntity querySkuDetail(String goodsId) {
        return repository.querySkuDetail(goodsId);
    }

}

