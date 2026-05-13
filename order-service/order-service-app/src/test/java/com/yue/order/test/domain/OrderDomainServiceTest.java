package com.yue.order.test.domain;

import com.yue.order.domain.order.adapter.port.*;
import com.yue.order.domain.order.adapter.repository.IOrderCacheRepository;
import com.yue.order.domain.order.adapter.repository.IOrderRepository;
import com.yue.order.domain.order.model.entity.CreateOrderCommand;
import com.yue.order.domain.order.model.entity.NormalOrderEnqueueResult;
import com.yue.order.domain.order.model.entity.OrderEntity;
import com.yue.order.domain.order.model.valobj.MarketTypeVO;
import com.yue.order.domain.order.model.valobj.OrderStatusVO;
import com.yue.order.domain.order.service.OrderDomainService;
import com.yue.order.types.exception.AppException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderDomainServiceTest {

    @Mock
    private IOrderRepository orderRepository;
    @Mock
    private IPayPort payPort;
    @Mock
    private IOrderPaidPublisher orderPaidPublisher;
    @Mock
    private IOrderClosePublisher orderClosePublisher;
    @Mock
    private IOrderRefundPublisher refundPublisher;
    @Mock
    private INormalOrderPendingPublisher normalOrderPendingPublisher;
    @Mock
    private IGroupBuyOrderPendingPublisher groupBuyOrderPendingPublisher;
    @Mock
    private IOrderCacheRepository orderCacheRepository;
    @Mock
    private IOrderShipTaskPublisher orderShipTaskPublisher;

    private OrderDomainService service;

    @BeforeEach
    void setUp() {
        service = new OrderDomainService();
        ReflectionTestUtils.setField(service, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(service, "payPort", payPort);
        ReflectionTestUtils.setField(service, "orderPaidPublisher", orderPaidPublisher);
        ReflectionTestUtils.setField(service, "orderClosePublisher", orderClosePublisher);
        ReflectionTestUtils.setField(service, "refundPublisher", refundPublisher);
        ReflectionTestUtils.setField(service, "normalOrderPendingPublisher", normalOrderPendingPublisher);
        ReflectionTestUtils.setField(service, "groupBuyOrderPendingPublisher", groupBuyOrderPendingPublisher);
        ReflectionTestUtils.setField(service, "orderCacheRepository", orderCacheRepository);
        ReflectionTestUtils.setField(service, "orderShipTaskPublisher", orderShipTaskPublisher);
        ReflectionTestUtils.setField(service, "allowDirectNormalCreateOrder", true);
        ReflectionTestUtils.setField(service, "getPayUrlPendingRetries", 1);
        ReflectionTestUtils.setField(service, "getPayUrlPendingWaitMs", 1L);
        ReflectionTestUtils.setField(service, "pendingMarkerTtlMinutes", 30L);
    }

    @Test
    void createOrderRejectsDirectNormalWhenDisabled() {
        ReflectionTestUtils.setField(service, "allowDirectNormalCreateOrder", false);

        assertThatThrownBy(() -> service.createOrder(createCommand("normal")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("直连 create_order 已关闭");
    }

    @Test
    void submitNormalOrderFromMallBuildsPendingMessage() {
        CreateOrderCommand command = createCommand("normal");

        NormalOrderEnqueueResult result = service.submitNormalOrderFromMall(command);

        assertThat(result.getOrderId()).isNotBlank();
        ArgumentCaptor<String> outTradeNoCaptor = ArgumentCaptor.forClass(String.class);
        verify(orderCacheRepository).markPending(eq("u1"), eq(result.getOrderId()), outTradeNoCaptor.capture(), any());
        assertThat(outTradeNoCaptor.getValue()).startsWith("OT");
        verify(normalOrderPendingPublisher).publishInsertSync(contains("\"marketType\":\"normal\""));
    }

    @Test
    void getPayUrlCallsPayPortAndPersistsUrl() {
        when(orderRepository.queryByUserIdAndOrderId("u1", "OID-3"))
                .thenReturn(OrderEntity.builder()
                        .userId("u1")
                        .orderId("OID-3")
                        .outTradeNo("OUT-3")
                        .goodsId("g1")
                        .originalPrice(new BigDecimal("100"))
                        .deductionPrice(new BigDecimal("10"))
                        .payPrice(new BigDecimal("90"))
                        .status(OrderStatusVO.LOCK)
                        .marketType(MarketTypeVO.NORMAL)
                        .build());
        when(payPort.getPayUrl(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn("pay-url");

        String result = service.getPayUrl("u1", "OID-3");

        assertThat(result).isEqualTo("pay-url");
        verify(orderRepository).updatePayUrl("u1", "OUT-3", "pay-url");
    }

    @Test
    void handlePaySuccessTriggersRefundForClosedOrder() {
        when(orderRepository.queryByOutTradeNo("OUT-4"))
                .thenReturn(OrderEntity.builder()
                        .userId("u1")
                        .orderId("OID-4")
                        .status(OrderStatusVO.CLOSE)
                        .build());

        service.handlePaySuccess("OUT-4", "group_buy", new Date(0));

        verify(refundPublisher).publishPayRefund("u1", "OUT-4", "group_buy");
        verify(orderRepository, never()).updatePaySuccess(anyString(), any());
    }

    @Test
    void handlePaySuccessPublishesPaidAndShipTaskForNormalOrder() {
        when(orderRepository.queryByOutTradeNo("OUT-5"))
                .thenReturn(OrderEntity.builder()
                        .userId("u2")
                        .orderId("OID-5")
                        .outTradeNo("OUT-5")
                        .status(OrderStatusVO.LOCK)
                        .marketType(MarketTypeVO.NORMAL)
                        .build());

        service.handlePaySuccess("OUT-5", "normal", new Date(0));

        verify(orderRepository).updatePaySuccess(eq("OUT-5"), any(Date.class));
        verify(orderPaidPublisher).publishOrderPaid(eq("u2"), eq("OID-5"), eq("OUT-5"), eq("normal"), any(Date.class));
        verify(orderShipTaskPublisher).publishOrderShipTask("u2", "OID-5", "OUT-5");
    }

    @Test
    void createOrderGroupBuyMarksRedisAndPublishesMq() {
        CreateOrderCommand command = createCommand("group_buy");

        String orderId = service.createOrder(command);

        assertThat(orderId).isNotBlank();
        ArgumentCaptor<String> outTradeNoCaptor = ArgumentCaptor.forClass(String.class);
        verify(orderCacheRepository).markPending(eq("u1"), eq(orderId), outTradeNoCaptor.capture(), any());
        assertThat(outTradeNoCaptor.getValue()).startsWith("OT");
        verify(groupBuyOrderPendingPublisher).publishInsertSync(contains("\"marketType\":\"group_buy\""));
        verify(orderRepository, never()).saveOrder(any());
    }

    @Test
    void createOrderGroupBuyClearsMarkerWhenMqFails() {
        CreateOrderCommand command = createCommand("group_buy");
        doThrow(new RuntimeException("mq down")).when(groupBuyOrderPendingPublisher).publishInsertSync(anyString());

        assertThatThrownBy(() -> service.createOrder(command))
                .isInstanceOf(RuntimeException.class);

        verify(orderCacheRepository).markPending(eq("u1"), anyString(), anyString(), any());
        verify(orderCacheRepository).clearPending(eq("u1"), anyString());
    }

    @Test
    void submitNormalOrderFromMallClearsMarkerWhenMqFails() {
        CreateOrderCommand command = createCommand("normal");
        doThrow(new RuntimeException("mq down")).when(normalOrderPendingPublisher).publishInsertSync(anyString());

        assertThatThrownBy(() -> service.submitNormalOrderFromMall(command))
                .isInstanceOf(RuntimeException.class);

        verify(orderCacheRepository).markPending(eq("u1"), anyString(), anyString(), any());
        verify(orderCacheRepository).clearPending(eq("u1"), anyString());
    }

    @Test
    void getPayUrlFailsFastWhenNoMarkerAndDbMiss() {
        when(orderRepository.queryByUserIdAndOrderId("u1", "MISSING")).thenReturn(null);
        when(orderCacheRepository.existsPending("u1", "MISSING")).thenReturn(false);

        assertThatThrownBy(() -> service.getPayUrl("u1", "MISSING"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("订单不存在");

        verify(orderRepository, times(1)).queryByUserIdAndOrderId("u1", "MISSING");
    }

    @Test
    void getPayUrlRetriesDbWhenMarkerExists() {
        ReflectionTestUtils.setField(service, "getPayUrlPendingRetries", 3);
        when(orderRepository.queryByUserIdAndOrderId("u1", "OID-LATE"))
                .thenReturn(null)
                .thenReturn(null)
                .thenReturn(OrderEntity.builder()
                        .userId("u1")
                        .orderId("OID-LATE")
                        .outTradeNo("OUT-LATE")
                        .goodsId("g1")
                        .originalPrice(new BigDecimal("100"))
                        .deductionPrice(new BigDecimal("10"))
                        .payPrice(new BigDecimal("90"))
                        .status(OrderStatusVO.LOCK)
                        .marketType(MarketTypeVO.GROUP_BUY)
                        .payUrl("cached-url")
                        .build());
        when(orderCacheRepository.existsPending("u1", "OID-LATE")).thenReturn(true);

        String result = service.getPayUrl("u1", "OID-LATE");

        assertThat(result).isEqualTo("cached-url");
        verify(orderRepository, times(3)).queryByUserIdAndOrderId("u1", "OID-LATE");
    }

    private static CreateOrderCommand createCommand(String marketType) {
        return CreateOrderCommand.builder()
                .userId("u1")
                .goodsId("g1")
                .goodsName("goods")
                .goodsImageUrl("img")
                .source("s01")
                .channel("c01")
                .originalPrice(new BigDecimal("100"))
                .deductionPrice(new BigDecimal("10"))
                .payPrice(new BigDecimal("90"))
                .marketType(marketType)
                .build();
    }
}
