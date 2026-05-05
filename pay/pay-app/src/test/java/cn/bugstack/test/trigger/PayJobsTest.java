package cn.bugstack.test.trigger;

import cn.bugstack.domain.order.adapter.port.IPaySuccessPublisher;
import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import cn.bugstack.domain.order.service.IOrderService;
import cn.bugstack.trigger.job.NoPayNotifyOrderJob;
import cn.bugstack.trigger.job.TimeoutCloseOrderJob;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PayJobsTest {

    @Test
    void timeoutCloseOrderJobClosesEachReconciledOrder() {
        IOrderService orderService = mock(IOrderService.class);
        when(orderService.queryPayReconcileCloseOrderList()).thenReturn(List.of("OID-1"));
        when(orderService.queryOrderByOrderId("OID-1")).thenReturn(OrderEntity.builder().marketType("normal").build());
        when(orderService.changeOrderClose("OID-1")).thenReturn(true);

        TimeoutCloseOrderJob job = new TimeoutCloseOrderJob();
        ReflectionTestUtils.setField(job, "orderService", orderService);
        job.exec();

        verify(orderService).changeOrderClose("OID-1");
    }

    @Test
    void noPayNotifyOrderJobQueriesAlipayAndPublishesSettlement() throws Exception {
        IOrderService orderService = mock(IOrderService.class);
        AlipayClient alipayClient = mock(AlipayClient.class);
        IPaySuccessPublisher publisher = mock(IPaySuccessPublisher.class);

        when(orderService.queryNoPayNotifyOrder()).thenReturn(List.of("OID-2"));
        when(orderService.queryOrderByOrderId("OID-2"))
                .thenReturn(OrderEntity.builder()
                        .userId("u1")
                        .marketType("group_buy")
                        .orderStatusVO(OrderStatusVO.PAY_WAIT)
                        .build());
        AlipayTradeQueryResponse queryResponse = new AlipayTradeQueryResponse();
        queryResponse.setCode("10000");
        queryResponse.setSendPayDate(new Date(0));
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class))).thenReturn(queryResponse);

        NoPayNotifyOrderJob job = new NoPayNotifyOrderJob();
        ReflectionTestUtils.setField(job, "orderService", orderService);
        ReflectionTestUtils.setField(job, "alipayClient", alipayClient);
        ReflectionTestUtils.setField(job, "paySuccessPublisher", publisher);

        job.exec();

        verify(orderService).changeOrderPaySuccess("OID-2", queryResponse.getSendPayDate());
        verify(publisher).sendSettlementMessage("u1", "OID-2", queryResponse.getSendPayDate(), "group_buy");
    }
}
