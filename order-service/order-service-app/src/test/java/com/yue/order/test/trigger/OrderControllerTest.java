package com.yue.order.test.trigger;

import com.yue.order.api.dto.*;
import com.yue.order.api.response.Response;
import com.yue.order.domain.order.model.entity.NormalOrderEnqueueResult;
import com.yue.order.domain.order.service.IOrderDomainService;
import com.yue.order.trigger.http.OrderController;
import com.yue.order.types.enums.ResponseCode;
import com.yue.order.types.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private IOrderDomainService orderDomainService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private OrderController controller;

    @BeforeEach
    void setUp() {
        controller = new OrderController();
        ReflectionTestUtils.setField(controller, "orderDomainService", orderDomainService);
        ReflectionTestUtils.setField(controller, "stringRedisTemplate", redisTemplate);
        ReflectionTestUtils.setField(controller, "internalCreateToken", "secret");
    }

    @Test
    void createOrderValidatesRequiredFields() {
        Response<CreateOrderResponseDTO> response = controller.createOrder(new CreateOrderRequestDTO());

        assertThat(response.getCode()).isEqualTo(ResponseCode.ILLEGAL_PARAMETER.getCode());
    }

    @Test
    void createOrderNormalFromMallRejectsBadToken() {
        Response<CreateOrderResponseDTO> response = controller.createOrderNormalFromMall(
                CreateOrderRequestDTO.builder().userId("u1").productId("g1").payPrice(new BigDecimal("1")).build(),
                "wrong");

        assertThat(response.getCode()).isEqualTo(ResponseCode.ILLEGAL_PARAMETER.getCode());
    }

    @Test
    void createOrderNormalFromMallReturnsOrderId() {
        when(orderDomainService.submitNormalOrderFromMall(any()))
                .thenReturn(NormalOrderEnqueueResult.builder().orderId("OID-1").build());

        Response<CreateOrderResponseDTO> response = controller.createOrderNormalFromMall(
                CreateOrderRequestDTO.builder().userId("u1").productId("g1").payPrice(new BigDecimal("1")).build(),
                "secret");

        assertThat(response.getCode()).isEqualTo(ResponseCode.SUCCESS.getCode());
        assertThat(response.getData().getOrderId()).isEqualTo("OID-1");
    }

    @Test
    void getPayUrlMapsBusinessException() {
        when(orderDomainService.getPayUrl("u1", "OID-2"))
                .thenThrow(new AppException("E001", "bad"));

        Response<String> response = controller.getPayUrl(GetPayUrlRequestDTO.builder().userId("u1").orderId("OID-2").build());

        assertThat(response.getCode()).isEqualTo("E001");
        assertThat(response.getInfo()).isEqualTo("bad");
    }

    @Test
    void querySeckillOrderReturnsOrderIdWhenTokenDone() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("seckill:token:status:token-1")).thenReturn("1");
        when(valueOperations.get("seckill:order:token-1")).thenReturn("OID-9");

        QuerySeckillOrderRequestDTO request = new QuerySeckillOrderRequestDTO();
        request.setSeckillToken("token-1");
        Response<QuerySeckillOrderResponseDTO> response = controller.querySeckillOrder(request);

        assertThat(response.getCode()).isEqualTo(ResponseCode.SUCCESS.getCode());
        assertThat(response.getData().getStatus()).isEqualTo(1);
        assertThat(response.getData().getOrderId()).isEqualTo("OID-9");
    }
}
