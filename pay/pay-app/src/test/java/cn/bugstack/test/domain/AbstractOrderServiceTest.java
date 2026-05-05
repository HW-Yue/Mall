package cn.bugstack.test.domain;

import cn.bugstack.domain.order.adapter.repository.IOrderRepository;
import cn.bugstack.domain.order.model.entity.CreateOrderEntity;
import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.valobj.MarketTypeVO;
import cn.bugstack.domain.order.service.AbstractOrderService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AbstractOrderServiceTest {

    @Test
    void createOrderReturnsExistingPayUrlWithoutSaving() throws Exception {
        IOrderRepository repository = mock(IOrderRepository.class);
        StubOrderService service = spy(new StubOrderService(repository));
        CreateOrderEntity command = createOrder("OID-1");

        when(repository.queryOrderByOrderId("OID-1"))
                .thenReturn(OrderEntity.builder().orderId("OID-1").payUrl("cached-pay-url").build());

        PayOrderEntity result = service.createOrder(command);

        assertThat(result.getOrderId()).isEqualTo("OID-1");
        assertThat(result.getPayUrl()).isEqualTo("cached-pay-url");
        verify(service, never()).doSaveOrder(any());
        verify(service, never()).doPrepayOrder(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createOrderSavesAndPrepaysWhenOrderDoesNotExist() throws Exception {
        IOrderRepository repository = mock(IOrderRepository.class);
        StubOrderService service = spy(new StubOrderService(repository));
        CreateOrderEntity command = createOrder("OID-2");

        when(repository.queryOrderByOrderId("OID-2")).thenReturn(null);
        doNothing().when(service).doSaveOrder(command);
        doReturn(PayOrderEntity.builder().orderId("OID-2").payUrl("new-pay-url").build())
                .when(service)
                .doPrepayOrder(anyString(), anyString(), anyString(), anyString(), any(), any(), any());

        PayOrderEntity result = service.createOrder(command);

        assertThat(result.getOrderId()).isEqualTo("OID-2");
        assertThat(result.getPayUrl()).isEqualTo("new-pay-url");
        verify(service).doSaveOrder(command);
        verify(service).doPrepayOrder(eq("u1"), eq("p1"), eq("goods"), eq("OID-2"), eq(new BigDecimal("88.00")),
                eq(MarketTypeVO.Normal), any());
    }

    private static CreateOrderEntity createOrder(String orderId) {
        return CreateOrderEntity.builder()
                .userId("u1")
                .productId("p1")
                .productName("goods")
                .orderId(orderId)
                .marketTypeVO(MarketTypeVO.Normal)
                .originalPrice(new BigDecimal("88.00"))
                .deductionPrice(BigDecimal.ZERO)
                .payPrice(new BigDecimal("88.00"))
                .build();
    }

    private static class StubOrderService extends AbstractOrderService {

        StubOrderService(IOrderRepository repository) {
            super(repository);
        }

        @Override
        protected void doSaveOrder(CreateOrderEntity createOrderEntity) {
        }

        @Override
        protected PayOrderEntity doPrepayOrder(String userId, String productId, String productName, String orderId,
                                               BigDecimal totalAmount, MarketTypeVO marketTypeVO,
                                               cn.bugstack.domain.order.model.entity.MarketPayDiscountEntity marketPayDiscountEntity) {
            return PayOrderEntity.builder().orderId(orderId).payUrl("pay-url").build();
        }

        @Override
        public void changeOrderPaySuccess(String orderId, java.util.Date payTime) {
        }

        @Override
        public java.util.List<String> queryNoPayNotifyOrder() {
            return java.util.List.of();
        }

        @Override
        public java.util.List<String> queryPayReconcileCloseOrderList() {
            return java.util.List.of();
        }

        @Override
        public boolean changeOrderClose(String orderId) {
            return false;
        }

        @Override
        public boolean changeOrderPayAfterClose(String orderId) {
            return false;
        }

        @Override
        public void changeOrderMarketSettlement(java.util.List<String> outTradeNoList) {
        }

        @Override
        public cn.bugstack.domain.order.model.entity.OrderEntity queryOrderByOrderId(String orderId) {
            return null;
        }

        @Override
        public String getPayUrl(String userId, String orderId) {
            return null;
        }

        @Override
        public boolean refundMarketOrder(String userId, String orderId) {
            return false;
        }

        @Override
        public boolean refundPayOrder(String userId, String orderId) {
            return false;
        }

        @Override
        public boolean closePayOrder(String outTradeNo) {
            return false;
        }

        @Override
        public boolean refundPayOrderByOutTradeNo(String outTradeNo) {
            return false;
        }
    }
}
