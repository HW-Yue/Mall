package com.yue.order.test.trigger;

import com.yue.order.domain.order.adapter.repository.IOrderRepository;
import com.yue.order.domain.order.model.entity.OrderEntity;
import com.yue.order.domain.order.service.IOrderDomainService;
import com.yue.order.trigger.listener.GroupBuySuccessNotifyListener;
import com.yue.order.trigger.listener.NormalOrderCreateListener;
import com.yue.order.trigger.listener.SeckillOrderCreateListener;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderListenersTest {

    @Test
    void groupBuySuccessNotifyDelegatesToDomainService() {
        IOrderDomainService domainService = mock(IOrderDomainService.class);
        GroupBuySuccessNotifyListener listener = new GroupBuySuccessNotifyListener();
        ReflectionTestUtils.setField(listener, "orderDomainService", domainService);

        listener.onMessage("{\"teamId\":\"T1\",\"orders\":[]}");

        verify(domainService).handleGroupBuySuccess("{\"teamId\":\"T1\",\"orders\":[]}");
    }

    @Test
    void normalOrderCreateListenerIgnoresInvalidMessageAndInsertsValidOrder() {
        IOrderRepository repository = mock(IOrderRepository.class);
        NormalOrderCreateListener listener = new NormalOrderCreateListener();
        ReflectionTestUtils.setField(listener, "orderRepository", repository);

        listener.onMessage("{\"userId\":\"u1\"}");
        verify(repository, never()).insertOrderWithoutMallLock(any());

        listener.onMessage("{\"orderId\":\"OID-1\",\"userId\":\"u1\",\"outTradeNo\":\"OUT-1\",\"payPrice\":99,\"goodsId\":\"g1\"}");
        verify(repository).insertOrderWithoutMallLock(any(OrderEntity.class));
    }

    @Test
    void seckillOrderCreateListenerWritesRedisResultAfterCreate() {
        IOrderDomainService domainService = mock(IOrderDomainService.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(domainService.createOrder(any())).thenReturn("OID-2");

        SeckillOrderCreateListener listener = new SeckillOrderCreateListener();
        ReflectionTestUtils.setField(listener, "orderDomainService", domainService);
        ReflectionTestUtils.setField(listener, "stringRedisTemplate", redisTemplate);

        listener.onMessage("{\"seckillToken\":\"token-1\",\"userId\":\"u1\",\"productId\":\"g1\",\"activityId\":1,\"payPrice\":\"19.9\"}");

        verify(valueOperations).set(eq("seckill:order:token-1"), eq("OID-2"), eq(120L), eq(java.util.concurrent.TimeUnit.MINUTES));
        verify(valueOperations).set(eq("seckill:token:status:token-1"), eq("1"), eq(120L), eq(java.util.concurrent.TimeUnit.MINUTES));
    }
}
