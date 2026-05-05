package com.yue.groupbuy.test.domain;

import com.yue.groupbuy.domain.trade.model.valobj.*;
import com.yue.groupbuy.types.enums.GroupBuyOrderEnumVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GroupBuyStateMachineTest {

    @Test
    void tradeOrderStateMachineHandlesLegalAndIllegalTransitions() {
        assertThat(GroupBuyTradeOrderStateMachine.next(TradeOrderStatusEnumVO.CREATE, GroupBuyTradeOrderEvent.PAID))
                .isEqualTo(TradeOrderStatusEnumVO.COMPLETE);
        assertThat(GroupBuyTradeOrderStateMachine.canTransition(TradeOrderStatusEnumVO.COMPLETE, GroupBuyTradeOrderEvent.REFUND_REQUEST))
                .isTrue();
        assertThat(GroupBuyTradeOrderStateMachine.next(TradeOrderStatusEnumVO.CLOSED, GroupBuyTradeOrderEvent.PAID)).isNull();
    }

    @Test
    void teamStateMachineHandlesLegalAndIllegalTransitions() {
        assertThat(GroupBuyTeamStateMachine.next(GroupBuyOrderEnumVO.PROGRESS, GroupBuyTeamEvent.FORMED))
                .isEqualTo(GroupBuyOrderEnumVO.COMPLETE);
        assertThat(GroupBuyTeamStateMachine.validEvents(GroupBuyOrderEnumVO.COMPLETE))
                .containsExactlyInAnyOrder(GroupBuyTeamEvent.PARTIAL_REFUND, GroupBuyTeamEvent.FULL_REFUND);
        assertThat(GroupBuyTeamStateMachine.next(GroupBuyOrderEnumVO.FAIL, GroupBuyTeamEvent.FORMED)).isNull();
    }
}
