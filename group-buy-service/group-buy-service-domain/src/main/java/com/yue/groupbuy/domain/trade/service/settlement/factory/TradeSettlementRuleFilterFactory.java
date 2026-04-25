package com.yue.groupbuy.domain.trade.service.settlement.factory;

import com.yue.groupbuy.domain.trade.model.entity.GroupBuyTeamEntity;
import com.yue.groupbuy.domain.trade.model.entity.MarketPayOrderEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import com.yue.groupbuy.domain.trade.service.settlement.filter.EndRuleFilter;
import com.yue.groupbuy.domain.trade.service.settlement.filter.OutTradeNoRuleFilter;
import com.yue.groupbuy.domain.trade.service.settlement.filter.SCRuleFilter;
import com.yue.groupbuy.domain.trade.service.settlement.filter.SettableRuleFilter;
import cn.bugstack.wrench.design.framework.link.model2.LinkArmory;
import cn.bugstack.wrench.design.framework.link.model2.chain.BusinessLinkedList;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * 拼团结算责任链：Wrench {@link BusinessLinkedList}，上下文类型为 {@link SettlementLinkContext}。
 */
@Slf4j
@Service
public class TradeSettlementRuleFilterFactory {

    @Bean("tradeSettlementRuleFilter")
    public BusinessLinkedList<TradeSettlementRuleCommandEntity, SettlementLinkContext, TradeSettlementRuleFilterBackEntity> tradeSettlementRuleFilter(
            SCRuleFilter scRuleFilter,
            OutTradeNoRuleFilter outTradeNoRuleFilter,
            SettableRuleFilter settableRuleFilter,
            EndRuleFilter endRuleFilter) {

        LinkArmory<TradeSettlementRuleCommandEntity, SettlementLinkContext, TradeSettlementRuleFilterBackEntity> linkArmory =
                new LinkArmory<>("交易结算规则过滤链", scRuleFilter, outTradeNoRuleFilter, settableRuleFilter, endRuleFilter);

        return linkArmory.getLogicLink();
    }

    /**
     * 结算责任链专用上下文，继承 Wrench {@link cn.bugstack.wrench.design.framework.link.model2.DynamicContext}。
     */
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class SettlementLinkContext extends cn.bugstack.wrench.design.framework.link.model2.DynamicContext {

        private MarketPayOrderEntity marketPayOrderEntity;
        private GroupBuyTeamEntity groupBuyTeamEntity;
    }
}
