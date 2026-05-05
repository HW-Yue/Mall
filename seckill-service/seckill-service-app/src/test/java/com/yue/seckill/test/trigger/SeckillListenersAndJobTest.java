package com.yue.seckill.test.trigger;

import com.yue.seckill.domain.activity.adapter.repository.ISeckillActivityRepository;
import com.yue.seckill.domain.activity.model.valobj.SeckillStockVO;
import com.yue.seckill.domain.trade.adapter.port.ISeckillStockDeductPort;
import com.yue.seckill.domain.trade.adapter.port.ISeckillStockPort;
import com.yue.seckill.trigger.job.SeckillStockPreheatJob;
import com.yue.seckill.trigger.listener.OrderCloseSeckillListener;
import com.yue.seckill.trigger.listener.OrderPaidSeckillListener;
import com.yue.seckill.trigger.listener.PayRefundSeckillListener;
import com.yue.seckill.trigger.listener.SeckillStockDeductListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeckillListenersAndJobTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ISeckillStockPort seckillStockPort;
    @Mock
    private ISeckillStockDeductPort seckillStockDeductPort;
    @Mock
    private ISeckillActivityRepository activityRepository;

    private OrderCloseSeckillListener closeListener;
    private OrderPaidSeckillListener paidListener;
    private PayRefundSeckillListener refundListener;
    private SeckillStockDeductListener deductListener;
    private SeckillStockPreheatJob preheatJob;

    @BeforeEach
    void setUp() {
        closeListener = new OrderCloseSeckillListener();
        ReflectionTestUtils.setField(closeListener, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(closeListener, "seckillStockPort", seckillStockPort);

        paidListener = new OrderPaidSeckillListener();
        ReflectionTestUtils.setField(paidListener, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(paidListener, "seckillStockPort", seckillStockPort);
        ReflectionTestUtils.setField(paidListener, "seckillStockDeductPort", seckillStockDeductPort);

        refundListener = new PayRefundSeckillListener();
        ReflectionTestUtils.setField(refundListener, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(refundListener, "seckillStockPort", seckillStockPort);
        ReflectionTestUtils.setField(refundListener, "seckillStockDeductPort", seckillStockDeductPort);

        deductListener = new SeckillStockDeductListener();
        ReflectionTestUtils.setField(deductListener, "seckillActivityRepository", activityRepository);

        preheatJob = new SeckillStockPreheatJob();
        ReflectionTestUtils.setField(preheatJob, "seckillActivityRepository", activityRepository);
        ReflectionTestUtils.setField(preheatJob, "seckillStockPort", seckillStockPort);
    }

    @Test
    void orderCloseListenerRestoresStockAndSkipsDuplicates() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("seckill:order:close:handled:OID-1", "1", 2, TimeUnit.HOURS)).thenReturn(true);
        when(valueOperations.get("seckill:order:meta:OID-1")).thenReturn("u1:g1:1001");

        closeListener.onMessage("{\"orderId\":\"OID-1\"}");

        verify(seckillStockPort).recoverStock(1001L, "g1");

        reset(seckillStockPort);
        when(valueOperations.setIfAbsent("seckill:order:close:handled:OID-2", "1", 2, TimeUnit.HOURS)).thenReturn(false);
        closeListener.onMessage("{\"orderId\":\"OID-2\"}");
        verifyNoInteractions(seckillStockPort);
    }

    @Test
    void orderPaidListenerDeductsRealStockAndWritesRefundIndex() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("seckill:order:meta:OID-1")).thenReturn("u1:g1:1001");
        when(seckillStockPort.deductRealStockByLua(1001L, "g1")).thenReturn(1);

        paidListener.onMessage("{\"orderId\":\"OID-1\",\"outTradeNo\":\"OTN-1\"}");

        verify(seckillStockDeductPort).sendDeductStockTask(1001L, "g1");
        verify(valueOperations).set("seckill:order:by-trade:OTN-1", "1001:g1", 24L, TimeUnit.HOURS);
    }

    @Test
    void payRefundListenerRestoresStockAndSkipsMismatchedMessages() {
        refundListener.onMessage("{\"marketType\":\"normal\",\"outTradeNo\":\"OTN-1\"}");
        verifyNoInteractions(seckillStockPort, seckillStockDeductPort);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("seckill:refunded:OTN-2"), anyString(), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);
        when(valueOperations.get("seckill:order:by-trade:OTN-2")).thenReturn("1001:g1");
        when(seckillStockPort.restoreFullStock(1001L, "g1")).thenReturn(11);

        refundListener.onMessage("{\"marketType\":\"seckill\",\"outTradeNo\":\"OTN-2\"}");

        verify(seckillStockPort).restoreFullStock(1001L, "g1");
        verify(seckillStockDeductPort).sendRecoverStockTask(1001L, "g1");
    }

    @Test
    void seckillStockDeductListenerDispatchesDeductAndRecover() {
        when(activityRepository.deductStock(1001L, "g1")).thenReturn(true);
        when(activityRepository.recoverStock(1001L, "g1")).thenReturn(true);

        deductListener.onMessage("{\"activityId\":1001,\"productId\":\"g1\",\"op\":\"deduct\"}");
        deductListener.onMessage("{\"activityId\":1001,\"productId\":\"g1\",\"op\":\"recover\"}");

        verify(activityRepository).deductStock(1001L, "g1");
        verify(activityRepository).recoverStock(1001L, "g1");
    }

    @Test
    void preheatJobLoadsAllStockRowsAndHandlesEmptyCase() throws Exception {
        when(activityRepository.querySeckillStockList()).thenReturn(List.of(
                SeckillStockVO.builder().activityId(1001L).goodsId("g1").remainCount(3).build(),
                SeckillStockVO.builder().activityId(1002L).goodsId("g2").remainCount(null).build()));

        preheatJob.run();

        verify(seckillStockPort).preloadStock(1001L, "g1", 3);
        verify(seckillStockPort).preloadStock(1002L, "g2", 0);

        reset(seckillStockPort);
        when(activityRepository.querySeckillStockList()).thenReturn(List.of());
        preheatJob.run();
        verifyNoInteractions(seckillStockPort);
    }
}
