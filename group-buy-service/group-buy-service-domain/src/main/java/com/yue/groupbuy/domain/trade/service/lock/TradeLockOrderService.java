package com.yue.groupbuy.domain.trade.service.lock;

import com.yue.groupbuy.domain.trade.adapter.repository.ITradeRepository;
import com.yue.groupbuy.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import com.yue.groupbuy.domain.trade.model.entity.*;
import com.yue.groupbuy.domain.trade.model.valobj.GroupBuyProgressVO;
import com.yue.groupbuy.domain.trade.service.ITradeLockOrderService;
import com.yue.groupbuy.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import cn.bugstack.wrench.design.framework.link.model2.chain.BusinessLinkedList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Slf4j
@Service
public class TradeLockOrderService implements ITradeLockOrderService {

    @Resource
    private ITradeRepository repository;
    @Resource
    private BusinessLinkedList<TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.TradeLockLinkContext, TradeLockRuleFilterBackEntity> tradeRuleFilter;

    @Override
    public MarketPayOrderEntity queryNoPayMarketPayOrderByOutTradeNo(String userId, String outTradeNo) {
        log.info("拼团交易-查询未支付营销订单 userId:{} outTradeNo:{}", userId, outTradeNo);
        return repository.queryMarketPayOrderEntityByOutTradeNo(userId, outTradeNo);
    }

    @Override
    public GroupBuyProgressVO queryGroupBuyProgress(String teamId) {
        log.info("拼团交易-查询拼单进度 teamId:{}", teamId);
        return repository.queryGroupBuyProgress(teamId);
    }

    @Override
    public MarketPayOrderEntity lockMarketPayOrder(UserEntity userEntity, PayActivityEntity payActivityEntity, PayDiscountEntity payDiscountEntity) throws Exception {
        log.info("拼团交易-锁定营销优惠支付订单 userId:{} activityId:{} goodsId:{}", userEntity.getUserId(), payActivityEntity.getActivityId(), payDiscountEntity.getGoodsId());

        TradeLockRuleFilterBackEntity back = tradeRuleFilter.apply(
                TradeLockRuleCommandEntity.builder()
                        .activityId(payActivityEntity.getActivityId())
                        .goodsId(payDiscountEntity.getGoodsId())
                        .userId(userEntity.getUserId())
                        .teamId(payActivityEntity.getTeamId())
                        .build(),
                new TradeLockRuleFilterFactory.TradeLockLinkContext());

        Integer userTakeOrderCount = back.getUserTakeOrderCount();

        GroupBuyOrderAggregate groupBuyOrderAggregate = GroupBuyOrderAggregate.builder()
                .userEntity(userEntity)
                .payActivityEntity(payActivityEntity)
                .payDiscountEntity(payDiscountEntity)
                .userTakeOrderCount(userTakeOrderCount)
                .build();

        try {
            return repository.lockMarketPayOrder(groupBuyOrderAggregate);
        } catch (Exception e) {
            repository.recoveryTeamStock(back.getRecoveryTeamStockKey(), payActivityEntity.getValidTime());
            throw e;
        }
    }

}
