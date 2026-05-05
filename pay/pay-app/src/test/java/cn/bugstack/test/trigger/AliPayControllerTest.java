package cn.bugstack.test.trigger;

import cn.bugstack.api.dto.CreatePayRequestDTO;
import cn.bugstack.api.response.Response;
import cn.bugstack.domain.order.adapter.port.IOrderRefundPublisher;
import cn.bugstack.domain.order.adapter.port.IPaySuccessPublisher;
import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import cn.bugstack.domain.order.service.IOrderService;
import cn.bugstack.trigger.http.AliPayController;
import com.alipay.api.AlipayClient;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AliPayControllerTest {

    @Mock
    private IOrderService orderService;
    @Mock
    private IPaySuccessPublisher paySuccessPublisher;
    @Mock
    private IOrderRefundPublisher orderRefundPublisher;
    @Mock
    private AlipayClient alipayClient;

    private AliPayController controller;

    @BeforeEach
    void setUp() {
        controller = new AliPayController();
        ReflectionTestUtils.setField(controller, "orderService", orderService);
        ReflectionTestUtils.setField(controller, "paySuccessPublisher", paySuccessPublisher);
        ReflectionTestUtils.setField(controller, "orderRefundPublisher", orderRefundPublisher);
        ReflectionTestUtils.setField(controller, "alipayClient", alipayClient);
        ReflectionTestUtils.setField(controller, "alipayPublicKey", "test-key");
    }

    @Test
    void createPayOrderReturnsSuccessPayload() throws Exception {
        when(orderService.createOrder(any())).thenReturn(PayOrderEntity.builder().orderId("OID-1").payUrl("pay-url").build());

        CreatePayRequestDTO request = new CreatePayRequestDTO();
        request.setUserId("u1");
        request.setProductId("p1");
        request.setProductName("goods");
        request.setOutTradeNo("OID-1");
        request.setOriginalPrice(new BigDecimal("100"));
        request.setDeductionPrice(new BigDecimal("10"));
        request.setPayPrice(new BigDecimal("90"));
        request.setMarketType("normal");

        Response<String> response = controller.createPayOrder(request);

        assertThat(response.getCode()).isEqualTo("0000");
        assertThat(response.getData()).isEqualTo("pay-url");
    }

    @Test
    void payNotifyRejectsNonSuccessTradeStatus() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("trade_status", "WAIT_BUYER_PAY");

        assertThat(controller.payNotify(request)).isEqualTo("false");
        verifyNoInteractions(orderService, paySuccessPublisher, orderRefundPublisher);
    }

    @Test
    void payNotifyHandlesClosedOrderByRefunding() throws Exception {
        MockHttpServletRequest request = successNotifyRequest("OID-2");
        when(orderService.queryOrderByOrderId("OID-2"))
                .thenReturn(OrderEntity.builder()
                        .userId("u2")
                        .marketType("group_buy")
                        .orderStatusVO(OrderStatusVO.CLOSE)
                        .build());
        when(orderService.refundPayOrder("u2", "OID-2")).thenReturn(true);

        String result = controller.payNotify(request);

        assertThat(result).isEqualTo("success");
        verify(orderService).changeOrderPayAfterClose("OID-2");
        verify(orderService).refundPayOrder("u2", "OID-2");
        verify(orderRefundPublisher).sendPayRefundMessage("u2", "OID-2", "group_buy");
        verify(paySuccessPublisher, never()).sendSettlementMessage(any(), any(), any(), any());
    }

    @Test
    void payNotifyPublishesSettlementForNormalPaidOrder() throws Exception {
        MockHttpServletRequest request = successNotifyRequest("OID-3");
        OrderEntity order = OrderEntity.builder()
                .userId("u3")
                .marketType("normal")
                .orderStatusVO(OrderStatusVO.PAY_WAIT)
                .build();
        when(orderService.queryOrderByOrderId("OID-3")).thenReturn(order);

        String result = controller.payNotify(request);

        assertThat(result).isEqualTo("success");
        verify(orderService).changeOrderPaySuccess(eq("OID-3"), any(Date.class));
        verify(paySuccessPublisher).sendSettlementMessage(eq("u3"), eq("OID-3"), any(Date.class), eq("normal"));
    }

    private MockHttpServletRequest successNotifyRequest(String outTradeNo) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("trade_status", "TRADE_SUCCESS");
        request.addParameter("out_trade_no", outTradeNo);
        request.addParameter("sign", "mock_sign_bypass");
        request.addParameter("subject", "goods");
        request.addParameter("total_amount", "99.00");
        request.addParameter("buyer_id", "buyer");
        request.addParameter("gmt_payment", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        request.addParameter("buyer_pay_amount", "99.00");
        return request;
    }
}
