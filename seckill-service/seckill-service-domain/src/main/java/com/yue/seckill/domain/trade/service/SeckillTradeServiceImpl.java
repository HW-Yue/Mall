package com.yue.seckill.domain.trade.service;

import com.yue.seckill.domain.activity.adapter.repository.ISeckillActivityRepository;
import com.yue.seckill.domain.activity.model.entity.SeckillActivityEntity;
import com.yue.seckill.domain.activity.model.valobj.SkuVO;
import com.yue.seckill.domain.trade.adapter.port.ISeckillOrderTaskPort;
import com.yue.seckill.domain.trade.adapter.port.IOrderServicePort;
import com.yue.seckill.domain.trade.adapter.port.ISeckillStockPort;
import com.yue.seckill.domain.trade.model.entity.SeckillOrderResultEntity;
import com.yue.seckill.types.model.SeckillOrderTaskMessage;
import com.yue.seckill.types.enums.ResponseCode;
import com.yue.seckill.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;

/**
 * 秒杀交易服务实现
 */
@Slf4j
@Service
public class SeckillTradeServiceImpl implements ISeckillTradeService {

    @Resource
    private ISeckillActivityRepository seckillActivityRepository;
    @Resource
    private ISeckillOrderTaskPort seckillOrderTaskPort;
    @Resource
    private IOrderServicePort orderServicePort;
    @Resource
    private ISeckillStockPort seckillStockPort;
    @Override
    public SeckillOrderResultEntity createSeckillOrder(String userId, String productId, Long activityId,
                                                       String source, String channel, String goodsName, String goodsImageUrl,
                                                       boolean isTest) {
        // 压测模式：跳过所有真实 IO，直接返回 mock token
        if (isTest) {
            String mockToken = "test-" + RandomStringUtils.randomAlphanumeric(28);
            log.info("[TEST] 秒杀下单压测模式 userId:{} mockToken:{}", userId, mockToken);
            return SeckillOrderResultEntity.builder().seckillToken(mockToken).build();
        }

        // 1. 查询活动
        SeckillActivityEntity activity = seckillActivityRepository.querySeckillActivity(activityId);
        if (activity == null) {
            throw new AppException(ResponseCode.ACTIVITY_NOT_FOUND);
        }

        // 2. 校验活动有效性
        if (activity.getStatus() != 1) {
            throw new AppException(ResponseCode.ACTIVITY_NOT_EFFECTIVE);
        }

        // 3. 使用 Lua 原子扣减 Redis 库存（削峰）
        String stockKey = "seckill:stock:" + activityId + ":" + productId;
        log.info("[秒杀锁单] 准备扣减库存 userId:{} activityId:{} productId:{} stockKey:{}", userId, activityId, productId, stockKey);
        int deductResult = seckillStockPort.deductByLua(activityId, productId, userId);
        log.info("[秒杀锁单] 库存扣减结果 userId:{} activityId:{} productId:{} result:{}", userId, activityId, productId, deductResult);

        if (deductResult == 0) {
            // 库存不足
            throw new AppException(ResponseCode.STOCK_INSUFFICIENT);
        }
        if (deductResult == -1) {
            // 重复购买
            throw new AppException(ResponseCode.REPEATED_REQUEST);
        }
        if (deductResult == -2) {
            // 库存 key 不存在（未预热）
            log.error("[秒杀锁单] 库存未预热 userId:{} activityId:{} productId:{}", userId, activityId, productId);
            throw new AppException(ResponseCode.STOCK_NOT_ENOUGH);
        }
        // deductResult == 1 表示扣减成功

        // 4. 查询 SKU 价格信息
        SkuVO skuVO = seckillActivityRepository.querySkuByGoodsId(productId);
        BigDecimal originalPrice = (skuVO != null && skuVO.getOriginalPrice() != null)
                ? skuVO.getOriginalPrice() : BigDecimal.ZERO;
        BigDecimal payPrice = (activity.getSeckillPrice() != null ? activity.getSeckillPrice() : BigDecimal.ZERO)
                .max(BigDecimal.ZERO);
        BigDecimal deductionPrice = originalPrice.subtract(payPrice).max(BigDecimal.ZERO);

        // 5. 生成秒杀令牌并写入 Redis
        String seckillToken = RandomStringUtils.randomAlphanumeric(32);
        seckillStockPort.saveSeckillToken(seckillToken, userId, productId, activityId);

        String orderId = null;
        try {
            SeckillOrderTaskMessage message = SeckillOrderTaskMessage.builder()
                    .seckillToken(seckillToken)
                    .userId(userId)
                    .productId(productId)
                    .activityId(activityId)
                    .source(source)
                    .channel(channel)
                    .goodsName(StringUtils.defaultIfBlank(goodsName, "秒杀商品"))
                    .goodsImageUrl(StringUtils.defaultString(goodsImageUrl))
                    .originalPrice(originalPrice)
                    .deductionPrice(deductionPrice)
                    .payPrice(payPrice)
                    .build();
            seckillOrderTaskPort.sendCreateOrderTask(message);
            log.info("秒杀建单任务已发送 userId:{} seckillToken:{} activityId:{}",
                    userId, seckillToken, activityId);
        } catch (Exception e) {
            log.warn("秒杀建单任务发送失败，开始回滚 userId:{} seckillToken:{} activityId:{}",
                    userId, seckillToken, activityId, e);
            seckillStockPort.rollbackSeckillOrder(activityId, productId, userId, seckillToken);
            throw new AppException(ResponseCode.CREATE_ORDER_FAILED);
        }

        log.info("秒杀下单成功 userId:{} seckillToken:{} activityId:{}",
                userId, seckillToken, activityId);
        return SeckillOrderResultEntity.builder()
                .seckillToken(seckillToken)
                .orderId(orderId)
                .build();
    }

    @Override
    public SeckillActivityEntity querySeckillActivity(Long activityId) {
        SeckillActivityEntity vo = seckillActivityRepository.querySeckillActivity(activityId);
        if (vo == null) return null;
        return SeckillActivityEntity.builder()
                .activityId(vo.getActivityId())
                .activityName(vo.getActivityName())
                .seckillPrice(vo.getSeckillPrice())
                .stockCount(vo.getStockCount())
                .remainCount(vo.getRemainCount())
                .takeLimitCount(vo.getTakeLimitCount())
                .status(vo.getStatus())
                .startTime(vo.getStartTime())
                .endTime(vo.getEndTime())
                .tagId(vo.getTagId())
                .tagScope(vo.getTagScope())
                .build();
    }

    @Override
    public SkuVO querySku(String goodsId) {
        return seckillActivityRepository.querySkuByGoodsId(goodsId);
    }

    @Override
    public void refund(String userId, String orderId) {
        orderServicePort.refundExecute(userId, orderId);
        log.info("秒杀订单退款成功 userId:{} orderId:{}", userId, orderId);
    }

}
