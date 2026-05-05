package cn.bugstack.test.domain;

import cn.bugstack.domain.order.adapter.port.IRefundReceiptPublisher;
import cn.bugstack.domain.order.adapter.repository.IOrderRepository;
import cn.bugstack.domain.order.model.entity.MarketPayDiscountEntity;
import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.valobj.MarketTypeVO;
import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import cn.bugstack.domain.order.service.OrderService;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceUnitTest {

    @Mock
    private IOrderRepository repository;
    @Mock
    private AlipayClient alipayClient;
    @Mock
    private IRefundReceiptPublisher refundReceiptPublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(repository);
        ReflectionTestUtils.setField(orderService, "alipayClient", alipayClient);
        ReflectionTestUtils.setField(orderService, "refundReceiptPublisher", refundReceiptPublisher);
        ReflectionTestUtils.setField(orderService, "notifyUrl", "http://notify");
        ReflectionTestUtils.setField(orderService, "returnUrl", "http://return");
    }

    @Test
    void doPrepayOrderBuildsRequestAndPersistsPayInfo() throws Exception {
        AlipayTradePagePayResponse response = new AlipayTradePagePayResponse();
        response.setBody("<form>ok</form>");
        when(alipayClient.pageExecute(any(AlipayTradePagePayRequest.class))).thenReturn(response);

        PayOrderEntity result = ReflectionTestUtils.invokeMethod(
                orderService,
                "doPrepayOrder",
                "u1",
                "p1",
                "goods",
                "OID-1",
                new BigDecimal("50"),
                MarketTypeVO.GroupBuyMarket,
                MarketPayDiscountEntity.builder()
                        .originalPrice(new BigDecimal("50"))
                        .deductionPrice(new BigDecimal("5"))
                        .payPrice(new BigDecimal("45"))
                        .build());

        assertThat(result.getOrderId()).isEqualTo("OID-1");
        assertThat(result.getPayUrl()).isEqualTo("<form>ok</form>");
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatusVO.PAY_WAIT);

        ArgumentCaptor<PayOrderEntity> captor = ArgumentCaptor.forClass(PayOrderEntity.class);
        verify(repository).updateOrderPayInfo(captor.capture());
        assertThat(captor.getValue().getPayAmount()).isEqualByComparingTo("45");
        assertThat(captor.getValue().getMarketType()).isEqualTo(MarketTypeVO.GroupBuyMarket.getCode());

        ArgumentCaptor<AlipayTradePagePayRequest> requestCaptor = ArgumentCaptor.forClass(AlipayTradePagePayRequest.class);
        verify(alipayClient).pageExecute(requestCaptor.capture());
        AlipayTradePagePayRequest request = requestCaptor.getValue();
        assertThat(request.getNotifyUrl()).isEqualTo("http://notify");
        assertThat(request.getReturnUrl()).isEqualTo("http://return");
        assertThat(request.getBizContent()).contains("OID-1", "goods", "45");
    }

    @Test
    void changeOrderPaySuccessSkipsAlreadyTerminalStatuses() {
        when(repository.queryOrderByOrderId("OID-2"))
                .thenReturn(OrderEntity.builder().orderStatusVO(OrderStatusVO.PAY_SUCCESS).build());

        orderService.changeOrderPaySuccess("OID-2", new Date());

        verify(repository, never()).changeOrderPaySuccess(anyString(), any());
    }

    @Test
    void closePayOrderClosesPayWaitOrder() {
        when(repository.queryOrderByOrderId("OID-3"))
                .thenReturn(OrderEntity.builder().orderStatusVO(OrderStatusVO.PAY_WAIT).build());
        when(repository.changeOrderClose("OID-3")).thenReturn(true);

        boolean result = orderService.closePayOrder("OID-3");

        assertThat(result).isTrue();
        verify(repository).changeOrderClose("OID-3");
    }

    @Test
    void refundPayOrderByOutTradeNoPublishesReceiptForNonPaidOrder() throws Exception {
        when(repository.queryOrderByOrderId("OID-4"))
                .thenReturn(OrderEntity.builder()
                        .userId("u1")
                        .orderId("OID-4")
                        .marketType("group_buy")
                        .payAmount(new BigDecimal("20"))
                        .orderStatusVO(OrderStatusVO.CLOSE)
                        .build());

        boolean result = orderService.refundPayOrderByOutTradeNo("OID-4");

        assertThat(result).isTrue();
        verify(refundReceiptPublisher).publishRefundReceipt("u1", "OID-4", "group_buy");
        verify(alipayClient, never()).execute(any());
    }

    @Test
    void refundPayOrderByOutTradeNoCallsAlipayAndPublishesReceiptForPaidOrder() throws Exception {
        when(repository.queryOrderByOrderId("OID-5"))
                .thenReturn(OrderEntity.builder()
                        .userId("u2")
                        .orderId("OID-5")
                        .marketType("normal")
                        .payAmount(new BigDecimal("30"))
                        .orderStatusVO(OrderStatusVO.PAY_SUCCESS)
                        .build());
        AlipayTradeRefundResponse refundResponse = new AlipayTradeRefundResponse();
        refundResponse.setCode("10000");
        refundResponse.setMsg("Success");
        doReturn(refundResponse).when(alipayClient).execute(any(com.alipay.api.request.AlipayTradeRefundRequest.class));

        boolean result = orderService.refundPayOrderByOutTradeNo("OID-5");

        assertThat(result).isTrue();
        verify(refundReceiptPublisher).publishRefundReceipt("u2", "OID-5", "normal");
        verify(alipayClient).execute(any(AlipayTradeRefundRequest.class));
    }
}
