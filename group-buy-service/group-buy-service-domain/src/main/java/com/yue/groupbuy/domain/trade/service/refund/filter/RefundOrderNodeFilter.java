package com.yue.groupbuy.domain.trade.service.refund.filter;

import com.yue.groupbuy.domain.trade.model.entity.*;
import com.yue.groupbuy.domain.trade.model.valobj.RefundTypeEnumVO;
import com.yue.groupbuy.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import com.yue.groupbuy.domain.trade.service.refund.business.IRefundOrderStrategy;
import com.yue.groupbuy.domain.trade.service.refund.factory.TradeRefundRuleFilterFactory;
import com.yue.groupbuy.types.enums.GroupBuyOrderEnumVO;
import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Map;

@Slf4j
@Service
public class RefundOrderNodeFilter implements ILogicHandler<TradeRefundCommandEntity, TradeRefundRuleFilterFactory.RefundLinkContext, TradeRefundBehaviorEntity> {

    @Resource
    private Map<String, IRefundOrderStrategy> refundOrderStrategyMap;

    @Override
    public TradeRefundBehaviorEntity apply(TradeRefundCommandEntity tradeRefundCommandEntity, TradeRefundRuleFilterFactory.RefundLinkContext dynamicContext) throws Exception {
        log.info("逆向流程-退单操作，退单策略处理 userId:{} orderId:{}", tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOrderId());

        MarketPayOrderEntity marketPayOrderEntity = dynamicContext.getMarketPayOrderEntity();
        TradeOrderStatusEnumVO tradeOrderStatusEnumVO = marketPayOrderEntity.getTradeOrderStatusEnumVO();

        GroupBuyTeamEntity groupBuyTeamEntity = dynamicContext.getGroupBuyTeamEntity();
        GroupBuyOrderEnumVO groupBuyOrderEnumVO = groupBuyTeamEntity.getStatus();

        RefundTypeEnumVO refundType = RefundTypeEnumVO.getRefundStrategy(groupBuyOrderEnumVO, tradeOrderStatusEnumVO);
        IRefundOrderStrategy refundOrderStrategy = refundOrderStrategyMap.get(refundType.getStrategy());

        refundOrderStrategy.refundOrder(TradeRefundOrderEntity.builder()
                .userId(tradeRefundCommandEntity.getUserId())
                .orderId(marketPayOrderEntity.getOrderId())
                .teamId(marketPayOrderEntity.getTeamId())
                .activityId(groupBuyTeamEntity.getActivityId())
                .build());

        return stop(
                tradeRefundCommandEntity,
                dynamicContext,
                TradeRefundBehaviorEntity.builder()
                        .userId(tradeRefundCommandEntity.getUserId())
                        .orderId(marketPayOrderEntity.getOrderId())
                        .teamId(marketPayOrderEntity.getTeamId())
                        .tradeRefundBehaviorEnum(TradeRefundBehaviorEntity.TradeRefundBehaviorEnum.SUCCESS)
                        .build());
    }

}
