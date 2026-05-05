package cn.bugstack.test.domain;

import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import cn.bugstack.domain.order.model.valobj.PayOrderEvent;
import cn.bugstack.domain.order.model.valobj.PayOrderStateMachine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PayOrderStateMachineTest {

    @Test
    void returnsTargetStatusForLegalTransition() {
        assertThat(PayOrderStateMachine.next(OrderStatusVO.PAY_WAIT, PayOrderEvent.PAY_SUCCESS))
                .isEqualTo(OrderStatusVO.PAY_SUCCESS);
        assertThat(PayOrderStateMachine.canTransition(OrderStatusVO.PAY_SUCCESS, PayOrderEvent.REFUND_REQUEST))
                .isTrue();
        assertThat(PayOrderStateMachine.validEvents(OrderStatusVO.CREATE))
                .containsExactly(PayOrderEvent.CREATE_DONE);
    }

    @Test
    void returnsNullForIllegalTransition() {
        assertThat(PayOrderStateMachine.next(OrderStatusVO.CLOSE, PayOrderEvent.PAY_SUCCESS)).isNull();
        assertThat(PayOrderStateMachine.canTransition(OrderStatusVO.CLOSE, PayOrderEvent.PAY_SUCCESS)).isFalse();
    }
}
