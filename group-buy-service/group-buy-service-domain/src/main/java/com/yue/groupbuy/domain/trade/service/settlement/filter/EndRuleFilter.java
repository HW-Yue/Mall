package com.yue.groupbuy.domain.trade.service.settlement.filter;

import com.yue.groupbuy.domain.trade.model.entity.GroupBuyTeamEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import com.yue.groupbuy.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EndRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.SettlementLinkContext, TradeSettlementRuleFilterBackEntity> {

    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter, TradeSettlementRuleFilterFactory.SettlementLinkContext dynamicContext) throws Exception {
        log.info("结算规则过滤-结束节点 userId:{} outTradeNo:{}", requestParameter.getUserId(), requestParameter.getOutTradeNo());

        GroupBuyTeamEntity groupBuyTeamEntity = dynamicContext.getGroupBuyTeamEntity();

        return stop(
                requestParameter,
                dynamicContext,
                TradeSettlementRuleFilterBackEntity.builder()
                        .teamId(groupBuyTeamEntity.getTeamId())
                        .activityId(groupBuyTeamEntity.getActivityId())
                        .targetCount(groupBuyTeamEntity.getTargetCount())
                        .completeCount(groupBuyTeamEntity.getCompleteCount())
                        .lockCount(groupBuyTeamEntity.getLockCount())
                        .status(groupBuyTeamEntity.getStatus())
                        .validStartTime(groupBuyTeamEntity.getValidStartTime())
                        .validEndTime(groupBuyTeamEntity.getValidEndTime())
                        .notifyConfigVO(groupBuyTeamEntity.getNotifyConfigVO())
                        .build());
    }

}
