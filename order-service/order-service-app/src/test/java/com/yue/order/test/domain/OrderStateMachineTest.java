package com.yue.order.test.domain;

import com.yue.order.domain.order.model.valobj.OrderEvent;
import com.yue.order.domain.order.model.valobj.OrderStateMachine;
import com.yue.order.domain.order.model.valobj.OrderStatusVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStateMachineTest {

    @Test
    void legalTransitionsAreExposed() {
        assertThat(OrderStateMachine.next(OrderStatusVO.LOCK, OrderEvent.PAID)).isEqualTo(OrderStatusVO.PAY_SUCCESS);
        assertThat(OrderStateMachine.validEvents(OrderStatusVO.PAY_SUCCESS))
                .containsExactlyInAnyOrder(OrderEvent.REFUND_REQUEST, OrderEvent.SHIP);
        assertThat(OrderStateMachine.canTransition(OrderStatusVO.WAIT_SHIP, OrderEvent.SHIP_DONE)).isTrue();
    }

    @Test
    void illegalTransitionReturnsNull() {
        assertThat(OrderStateMachine.next(OrderStatusVO.CLOSE, OrderEvent.PAID)).isNull();
        assertThat(OrderStateMachine.canTransition(OrderStatusVO.CLOSE, OrderEvent.PAID)).isFalse();
    }
}
