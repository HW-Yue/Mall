package com.yue.groupbuy.domain.trade.service.refund.factory;

import com.yue.groupbuy.domain.trade.model.entity.GroupBuyTeamEntity;
import com.yue.groupbuy.domain.trade.model.entity.MarketPayOrderEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeRefundBehaviorEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeRefundCommandEntity;
import com.yue.groupbuy.domain.trade.service.refund.filter.DataNodeFilter;
import com.yue.groupbuy.domain.trade.service.refund.filter.RefundOrderNodeFilter;
import com.yue.groupbuy.domain.trade.service.refund.filter.UniqueRefundNodeFilter;
import cn.bugstack.wrench.design.framework.link.model2.LinkArmory;
import cn.bugstack.wrench.design.framework.link.model2.chain.BusinessLinkedList;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * 拼团退单责任链：Wrench {@link BusinessLinkedList}，上下文类型为 {@link RefundLinkContext}。
 */
@Slf4j
@Service
public class TradeRefundRuleFilterFactory {

    @Bean("tradeRefundRuleFilter")
    public BusinessLinkedList<TradeRefundCommandEntity, RefundLinkContext, TradeRefundBehaviorEntity> tradeRefundRuleFilter(
            DataNodeFilter dataNodeFilter,
            UniqueRefundNodeFilter uniqueRefundNodeFilter,
            RefundOrderNodeFilter refundOrderNodeFilter) {

        LinkArmory<TradeRefundCommandEntity, RefundLinkContext, TradeRefundBehaviorEntity> linkArmory =
                new LinkArmory<>("退单规则过滤链",
                        dataNodeFilter,
                        uniqueRefundNodeFilter,
                        refundOrderNodeFilter);

        return linkArmory.getLogicLink();
    }

    /**
     * 退单责任链专用上下文，继承 Wrench {@link cn.bugstack.wrench.design.framework.link.model2.DynamicContext}。
     */
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class RefundLinkContext extends cn.bugstack.wrench.design.framework.link.model2.DynamicContext {

        private MarketPayOrderEntity marketPayOrderEntity;
        private GroupBuyTeamEntity groupBuyTeamEntity;
    }
}
