package com.yue.seckill.domain.activity.service;

import com.yue.seckill.domain.activity.adapter.port.ISeckillGoodsCachePort;
import com.yue.seckill.domain.activity.adapter.repository.ISeckillActivityRepository;
import com.yue.seckill.domain.activity.model.entity.SeckillGoodsEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 秒杀市场服务实现
 * 查询链路：Caffeine(10s) → Redis(5min) → Redisson 分布式锁 + 看门狗 → DB
 */
@Slf4j
@Service
public class SeckillMarketServiceImpl implements ISeckillMarketService {

    @Resource
    private ISeckillActivityRepository seckillActivityRepository;
    @Resource
    private ISeckillGoodsCachePort seckillGoodsCachePort;

    @Override
    public List<SeckillGoodsEntity> querySeckillGoodsList() {
        return seckillGoodsCachePort.getGoodsList(seckillActivityRepository::querySeckillGoodsList);
    }

}
