package com.yue.groupbuy.domain.trade.service.lock.factory;

import com.yue.groupbuy.domain.trade.model.entity.GroupBuyActivityEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeLockRuleCommandEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import com.yue.groupbuy.domain.trade.service.lock.filter.ActivityUsabilityRuleFilter;
import com.yue.groupbuy.domain.trade.service.lock.filter.TeamStockOccupyRuleFilter;
import com.yue.groupbuy.domain.trade.service.lock.filter.UserTakeLimitRuleFilter;
import cn.bugstack.wrench.design.framework.link.model2.LinkArmory;
import cn.bugstack.wrench.design.framework.link.model2.chain.BusinessLinkedList;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * 拼团锁单责任链：由 Wrench {@link BusinessLinkedList} 组装，与 {@link TradeLockLinkContext} 配合使用。
 */
@Slf4j
@Service
public class TradeLockRuleFilterFactory {

    private static final String teamStockKey = "group_buy_market_team_stock_key_";

    @Bean("tradeRuleFilter")
    public BusinessLinkedList<TradeLockRuleCommandEntity, TradeLockLinkContext, TradeLockRuleFilterBackEntity> tradeRuleFilter(
            ActivityUsabilityRuleFilter activityUsabilityRuleFilter,
            UserTakeLimitRuleFilter userTakeLimitRuleFilter,
            TeamStockOccupyRuleFilter teamStockOccupyRuleFilter) {

        LinkArmory<TradeLockRuleCommandEntity, TradeLockLinkContext, TradeLockRuleFilterBackEntity> linkArmory =
                new LinkArmory<>("交易规则过滤链",
                        activityUsabilityRuleFilter,
                        userTakeLimitRuleFilter,
                        teamStockOccupyRuleFilter);

        return linkArmory.getLogicLink();
    }

    /**
     * 锁单责任链专用上下文：继承 Wrench {@link cn.bugstack.wrench.design.framework.link.model2.DynamicContext}，
     * 使 {@link BusinessLinkedList#apply} 能识别 {@code next}/{@code stop} 与 {@code proceed}。链上携带的
     * {@link #groupBuyActivity}、{@link #userTakeOrderCount} 等仍为拼团域业务态。
     */
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class TradeLockLinkContext extends cn.bugstack.wrench.design.framework.link.model2.DynamicContext {

        private GroupBuyActivityEntity groupBuyActivity;
        private Integer userTakeOrderCount;

        public String generateTeamStockKey(String teamId) {
            if (StringUtils.isBlank(teamId)) return null;
            return TradeLockRuleFilterFactory.generateTeamStockKey(groupBuyActivity.getActivityId(), teamId);
        }

        public String generateRecoveryTeamStockKey(String teamId) {
            if (StringUtils.isBlank(teamId)) return null;
            return TradeLockRuleFilterFactory.generateRecoveryTeamStockKey(groupBuyActivity.getActivityId(), teamId);
        }
    }

    public static String generateTeamStockKey(Long activityId, String teamId) {
        return teamStockKey + activityId + "_" + teamId;
    }

    public static String generateRecoveryTeamStockKey(Long activityId, String teamId) {
        return teamStockKey + activityId + "_" + teamId + "_recovery";
    }
}
