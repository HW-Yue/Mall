package com.yue.groupbuy.domain.trade.service.refund.filter;

import com.yue.groupbuy.domain.trade.adapter.repository.ITradeRepository;
import com.yue.groupbuy.domain.trade.model.entity.GroupBuyTeamEntity;
import com.yue.groupbuy.domain.trade.model.entity.MarketPayOrderEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeRefundBehaviorEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeRefundCommandEntity;
import com.yue.groupbuy.domain.trade.service.refund.factory.TradeRefundRuleFilterFactory;
import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Slf4j
@Service
public class DataNodeFilter implements ILogicHandler<TradeRefundCommandEntity, TradeRefundRuleFilterFactory.RefundLinkContext, TradeRefundBehaviorEntity> {

    @Resource
    private ITradeRepository repository;

    @Override
    public TradeRefundBehaviorEntity apply(TradeRefundCommandEntity tradeRefundCommandEntity, TradeRefundRuleFilterFactory.RefundLinkContext dynamicContext) throws Exception {
        log.info("逆向流程-退单操作，数据加载节点 userId:{} outTradeNo:{}", tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());

        MarketPayOrderEntity marketPayOrderEntity = repository.queryMarketPayOrderEntityByOutTradeNo(tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());
        String teamId = marketPayOrderEntity.getTeamId();

        GroupBuyTeamEntity groupBuyTeamEntity = repository.queryGroupBuyTeamByTeamId(teamId);

        dynamicContext.setMarketPayOrderEntity(marketPayOrderEntity);
        dynamicContext.setGroupBuyTeamEntity(groupBuyTeamEntity);

        return next(tradeRefundCommandEntity, dynamicContext);
    }

}
