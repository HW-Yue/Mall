package com.yue.groupbuy.test.trigger;

import com.yue.groupbuy.domain.trade.service.IGroupBuyDomainService;
import com.yue.groupbuy.trigger.listener.GroupBuyTimeoutRefundListener;
import com.yue.groupbuy.trigger.listener.OrderCloseGroupBuyListener;
import com.yue.groupbuy.trigger.listener.OrderPaidGroupBuyListener;
import com.yue.groupbuy.trigger.listener.PayRefundGroupBuyListener;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GroupBuyListenersTest {

    @Test
    void timeoutRefundListenerDelegatesByTeamId() {
        IGroupBuyDomainService service = mock(IGroupBuyDomainService.class);
        GroupBuyTimeoutRefundListener listener = new GroupBuyTimeoutRefundListener();
        ReflectionTestUtils.setField(listener, "groupBuyDomainService", service);

        listener.onMessage("{\"teamId\":\"T1\"}");

        verify(service).handleTimeoutRefund("T1");
    }

    @Test
    void orderCloseListenerSkipsWrongMarketTypeAndHandlesMatchingMessage() {
        IGroupBuyDomainService service = mock(IGroupBuyDomainService.class);
        OrderCloseGroupBuyListener listener = new OrderCloseGroupBuyListener();
        ReflectionTestUtils.setField(listener, "groupBuyDomainService", service);

        listener.onMessage("{\"marketType\":\"normal\",\"outTradeNo\":\"OUT-1\"}");
        verify(service, never()).handleOrderClose(any());

        listener.onMessage("{\"marketType\":\"group_buy\",\"outTradeNo\":\"OUT-2\"}");
        verify(service).handleOrderClose("OUT-2");
    }

    @Test
    void orderPaidAndRefundListenersDelegate() {
        IGroupBuyDomainService service = mock(IGroupBuyDomainService.class);

        OrderPaidGroupBuyListener paidListener = new OrderPaidGroupBuyListener();
        ReflectionTestUtils.setField(paidListener, "groupBuyDomainService", service);
        paidListener.onMessage("{\"userId\":\"u1\",\"outTradeNo\":\"OUT-3\",\"outTradeTime\":0}");
        verify(service).settlementGroupBuyOrder(any());

        PayRefundGroupBuyListener refundListener = new PayRefundGroupBuyListener();
        ReflectionTestUtils.setField(refundListener, "groupBuyDomainService", service);
        refundListener.onMessage("{\"outTradeNo\":\"OUT-4\"}");
        verify(service).handlePayRefund("OUT-4");
    }
}
