package com.yue.test.trigger;

import com.yue.api.dto.SkuStockRequestDTO;
import com.yue.api.response.Response;
import com.yue.order.api.IOrderDubboService;
import com.yue.order.api.dto.CreateOrderRequestDTO;
import com.yue.order.api.dto.CreateOrderResponseDTO;
import com.yue.trigger.http.OrderTradeController;
import com.yue.trigger.service.order.NormalOrderAntiFraudService;
import com.yue.trigger.service.sku.SkuStockAppService;
import com.yue.types.enums.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderTradeControllerTest {

    @Mock
    private NormalOrderAntiFraudService normalOrderAntiFraudService;
    @Mock
    private SkuStockAppService skuStockAppService;
    @Mock
    private IOrderDubboService orderDubboService;

    private OrderTradeController controller;

    @BeforeEach
    void setUp() {
        controller = new OrderTradeController();
        ReflectionTestUtils.setField(controller, "normalOrderAntiFraudService", normalOrderAntiFraudService);
        ReflectionTestUtils.setField(controller, "skuStockAppService", skuStockAppService);
        ReflectionTestUtils.setField(controller, "orderDubboService", orderDubboService);
    }

    @Test
    void createNormalOrderRejectsIllegalRequest() {
        Response<CreateOrderResponseDTO> response = controller.createNormalOrder(new CreateOrderRequestDTO());

        assertThat(response.getCode()).isEqualTo(ResponseCode.ILLEGAL_PARAMETER.getCode());
        assertThat(response.getInfo()).contains("userId / productId");
    }

    @Test
    void createNormalOrderRejectsMissingPayPrice() {
        CreateOrderRequestDTO request = baseRequest();
        request.setPayPrice(null);

        Response<CreateOrderResponseDTO> response = controller.createNormalOrder(request);

        assertThat(response.getCode()).isEqualTo(ResponseCode.ILLEGAL_PARAMETER.getCode());
        assertThat(response.getInfo()).contains("payPrice");
    }

    @Test
    void createNormalOrderRejectsByAntiFraud() {
        CreateOrderRequestDTO request = baseRequest();
        when(normalOrderAntiFraudService.checkOrNull("u1", "g1")).thenReturn("blocked");

        Response<CreateOrderResponseDTO> response = controller.createNormalOrder(request);

        assertThat(response.getCode()).isEqualTo(ResponseCode.RATE_LIMITER.getCode());
        assertThat(response.getInfo()).isEqualTo("blocked");
        verifyNoInteractions(skuStockAppService, orderDubboService);
    }

    @Test
    void createNormalOrderReturnsWhenLockStockFails() {
        CreateOrderRequestDTO request = baseRequest();
        when(normalOrderAntiFraudService.checkOrNull("u1", "g1")).thenReturn(null);
        when(skuStockAppService.lockStock(any(SkuStockRequestDTO.class)))
                .thenReturn(Response.<Boolean>builder().code(ResponseCode.UN_ERROR.getCode()).info("锁库失败").data(false).build());

        Response<CreateOrderResponseDTO> response = controller.createNormalOrder(request);

        assertThat(response.getCode()).isEqualTo(ResponseCode.E0009.getCode());
        assertThat(response.getInfo()).isEqualTo("锁库失败");
        verifyNoInteractions(orderDubboService);
    }

    @Test
    void createNormalOrderUnlocksWhenOrderServiceReturnsNull() {
        CreateOrderRequestDTO request = baseRequest();
        mockLockSuccess(request);
        when(orderDubboService.createOrderNormalFromMall(any())).thenReturn(null);

        Response<CreateOrderResponseDTO> response = controller.createNormalOrder(request);

        assertThat(response.getCode()).isEqualTo(ResponseCode.UN_ERROR.getCode());
        assertThat(response.getInfo()).contains("orderId");
        verify(skuStockAppService).unlockStock(any(SkuStockRequestDTO.class));
    }

    @Test
    void createNormalOrderUnlocksWhenOrderServiceFails() {
        CreateOrderRequestDTO request = baseRequest();
        mockLockSuccess(request);
        when(orderDubboService.createOrderNormalFromMall(any()))
                .thenThrow(new RuntimeException("创建订单失败"));

        Response<CreateOrderResponseDTO> response = controller.createNormalOrder(request);

        assertThat(response.getCode()).isEqualTo(ResponseCode.HTTP_EXCEPTION.getCode());
        assertThat(response.getInfo()).contains("创建订单失败");
        verify(skuStockAppService).unlockStock(any(SkuStockRequestDTO.class));
    }

    @Test
    void createNormalOrderUnlocksWhenOrderIdMissing() {
        CreateOrderRequestDTO request = baseRequest();
        mockLockSuccess(request);
        when(orderDubboService.createOrderNormalFromMall(any()))
                .thenReturn(new CreateOrderResponseDTO());

        Response<CreateOrderResponseDTO> response = controller.createNormalOrder(request);

        assertThat(response.getCode()).isEqualTo(ResponseCode.UN_ERROR.getCode());
        assertThat(response.getInfo()).contains("orderId");
        verify(skuStockAppService).unlockStock(any(SkuStockRequestDTO.class));
    }

    @Test
    void createNormalOrderUnlocksWhenDubboThrows() {
        CreateOrderRequestDTO request = baseRequest();
        mockLockSuccess(request);
        when(orderDubboService.createOrderNormalFromMall(any())).thenThrow(new IllegalStateException("downstream"));

        Response<CreateOrderResponseDTO> response = controller.createNormalOrder(request);

        assertThat(response.getCode()).isEqualTo(ResponseCode.HTTP_EXCEPTION.getCode());
        assertThat(response.getInfo()).contains("downstream");
        verify(skuStockAppService).unlockStock(any(SkuStockRequestDTO.class));
    }

    @Test
    void createNormalOrderBuildsNormalRequestAndReturnsSuccess() {
        CreateOrderRequestDTO request = baseRequest();
        mockLockSuccess(request);
        CreateOrderResponseDTO order = CreateOrderResponseDTO.builder().orderId("OID-1").outTradeNo("OTN-1").build();
        when(orderDubboService.createOrderNormalFromMall(any())).thenReturn(order);

        Response<CreateOrderResponseDTO> response = controller.createNormalOrder(request);

        assertThat(response.getCode()).isEqualTo(ResponseCode.SUCCESS.getCode());
        assertThat(response.getData()).isSameAs(order);
        ArgumentCaptor<CreateOrderRequestDTO> captor = ArgumentCaptor.forClass(CreateOrderRequestDTO.class);
        verify(orderDubboService).createOrderNormalFromMall(captor.capture());
        assertThat(captor.getValue().getMarketType()).isEqualTo("normal");
        verify(skuStockAppService, never()).unlockStock(any(SkuStockRequestDTO.class));
    }

    private CreateOrderRequestDTO baseRequest() {
        return CreateOrderRequestDTO.builder()
                .userId("u1")
                .productId("g1")
                .goodsName("phone")
                .payPrice(new BigDecimal("99"))
                .originalPrice(new BigDecimal("99"))
                .deductionPrice(BigDecimal.ZERO)
                .source("s01")
                .channel("c01")
                .build();
    }

    private void mockLockSuccess(CreateOrderRequestDTO request) {
        when(normalOrderAntiFraudService.checkOrNull(request.getUserId(), request.getProductId())).thenReturn(null);
        when(skuStockAppService.lockStock(any(SkuStockRequestDTO.class)))
                .thenReturn(Response.<Boolean>builder().code(ResponseCode.SUCCESS.getCode()).info("成功").data(true).build());
    }
}
