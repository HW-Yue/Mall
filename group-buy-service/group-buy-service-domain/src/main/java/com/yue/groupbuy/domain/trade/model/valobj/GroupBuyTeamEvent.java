package com.yue.groupbuy.domain.trade.model.valobj;

/**
 * 拼团团队聚合（team_order）状态机触发事件。
 * 配合 {@link GroupBuyTeamStateMachine}。
 */
public enum GroupBuyTeamEvent {

    /** 成团（complete_count == target_count） */
    FORMED,
    /** 团级超时未成团 */
    TIMEOUT,
    /** 已成团后部分用户退款 */
    PARTIAL_REFUND,
    /** 已成团后全部退款 */
    FULL_REFUND,

}
