package com.yue.seckill.test.infrastructure;

import com.yue.order.api.IOrderDubboService;
import com.yue.order.api.dto.CreateOrderResponseDTO;
import com.yue.order.api.dto.QueryOrderByOutTradeNoRequestDTO;
import com.yue.order.api.dto.RefundRequestDTO;
import com.yue.seckill.infrastructure.adapter.port.OrderServicePort;
import com.yue.seckill.infrastructure.adapter.port.SeckillOrderTaskPort;
import com.yue.seckill.infrastructure.adapter.port.SeckillStockDeductPort;
import com.yue.seckill.infrastructure.adapter.port.SeckillStockPort;
import com.yue.seckill.types.exception.AppException;
import com.yue.seckill.types.model.SeckillOrderTaskMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeckillPortsTest {

    @Mock
    private RocketMQTemplate rocketMQTemplate;
    @Mock
    private IOrderDubboService orderDubboService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private SeckillOrderTaskPort seckillOrderTaskPort;
    private SeckillStockDeductPort seckillStockDeductPort;
    private OrderServicePort orderServicePort;
    private SeckillStockPort seckillStockPort;

    @BeforeEach
    void setUp() {
        seckillOrderTaskPort = new SeckillOrderTaskPort();
        ReflectionTestUtils.setField(seckillOrderTaskPort, "rocketMQTemplate", rocketMQTemplate);
        ReflectionTestUtils.setField(seckillOrderTaskPort, "topic", "seckill-order-create");

        seckillStockDeductPort = new SeckillStockDeductPort();
        ReflectionTestUtils.setField(seckillStockDeductPort, "rocketMQTemplate", rocketMQTemplate);
        ReflectionTestUtils.setField(seckillStockDeductPort, "topic", "seckill-stock-deduct");

        orderServicePort = new OrderServicePort();
        ReflectionTestUtils.setField(orderServicePort, "orderDubboService", orderDubboService);

        seckillStockPort = new SeckillStockPort();
        ReflectionTestUtils.setField(seckillStockPort, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(seckillStockPort, "redissonClient", redissonClient);
    }

    @Test
    void seckillOrderTaskPortPublishesCreateOrderPayload() {
        SeckillOrderTaskMessage message = SeckillOrderTaskMessage.builder()
                .seckillToken("token-1")
                .userId("u1")
                .productId("g1")
                .activityId(1001L)
                .payPrice(new BigDecimal("80"))
                .build();

        seckillOrderTaskPort.sendCreateOrderTask(message);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(rocketMQTemplate).convertAndSend(eq("seckill-order-create"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("\"seckillToken\":\"token-1\"");
        assertThat(payloadCaptor.getValue()).contains("\"activityId\":1001");
    }

    @Test
    void seckillStockDeductPortPublishesDeductAndRecoverPayload() {
        seckillStockDeductPort.sendDeductStockTask(1001L, "g1");
        seckillStockDeductPort.sendRecoverStockTask(1001L, "g1");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(rocketMQTemplate, times(2)).convertAndSend(eq("seckill-stock-deduct"), payloadCaptor.capture());
        assertThat(payloadCaptor.getAllValues().get(0)).contains("\"op\":\"deduct\"");
        assertThat(payloadCaptor.getAllValues().get(1)).contains("\"op\":\"recover\"");
    }

    @Test
    void orderServicePortMapsQueryAndRefundResponses() {
        CreateOrderResponseDTO queryDto = CreateOrderResponseDTO.builder().orderId("OID-1").build();
        when(orderDubboService.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequestDTO.class))).thenReturn(queryDto);

        String orderId = orderServicePort.queryOrderIdByOutTradeNo("u1", "OTN-1");

        assertThat(orderId).isEqualTo("OID-1");

        when(orderDubboService.refundExecute(any(RefundRequestDTO.class))).thenReturn(true);
        orderServicePort.refundExecute("u1", "OID-1");

        ArgumentCaptor<QueryOrderByOutTradeNoRequestDTO> queryCaptor = ArgumentCaptor.forClass(QueryOrderByOutTradeNoRequestDTO.class);
        verify(orderDubboService).queryOrderByOutTradeNo(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getUserId()).isEqualTo("u1");
        assertThat(queryCaptor.getValue().getOutTradeNo()).isEqualTo("OTN-1");
    }

    @Test
    void orderServicePortReturnsNullOrThrowsOnBadResponses() {
        when(orderDubboService.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequestDTO.class))).thenThrow(new IllegalStateException("downstream"));

        assertThat(orderServicePort.queryOrderIdByOutTradeNo("u1", "OTN-2")).isNull();

        when(orderDubboService.refundExecute(any(RefundRequestDTO.class))).thenThrow(new RuntimeException("refund rpc error"));

        assertThatThrownBy(() -> orderServicePort.refundExecute("u1", "OID-2"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("退款执行失败");
    }

    @Test
    void seckillStockPortWritesDualStockAndTokenState() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        seckillStockPort.preloadStock(1001L, "g1", 5);
        seckillStockPort.preloadStock(1002L, "g2", 8, 60);
        seckillStockPort.saveSeckillToken("token-1", "u1", "g1", 1001L);
        seckillStockPort.recoverStock(1001L, "g1");
        doReturn(1L).when(stringRedisTemplate).execute(any(DefaultRedisScript.class), anyList(), anyString());

        seckillStockPort.rollbackSeckillOrder(1001L, "g1", "u1", "token-1");

        verify(valueOperations).set("seckill:stock:available:1001:g1", "5");
        verify(valueOperations).set("seckill:stock:real:1001:g1", "5");
        verify(valueOperations).set("seckill:stock:available:1002:g2", "8", 60L, TimeUnit.SECONDS);
        verify(valueOperations).set("seckill:stock:real:1002:g2", "8", 60L, TimeUnit.SECONDS);
        verify(valueOperations).set(eq("seckill:token:token-1"), eq("u1:g1:1001"), eq(24L), eq(TimeUnit.HOURS));
        verify(valueOperations).set(eq("seckill:token:status:token-1"), eq("0"), eq(24L), eq(TimeUnit.HOURS));
        verify(valueOperations).increment("seckill:stock:available:1001:g1");
        verify(stringRedisTemplate).delete("seckill:token:token-1");
        verify(stringRedisTemplate).delete("seckill:token:status:token-1");
    }
}
