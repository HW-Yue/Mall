package com.yue.test.trigger;

import com.yue.trigger.service.order.NormalOrderAntiFraudService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NormalOrderAntiFraudServiceTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RBucket<String> dedupBucket;
    @Mock
    private RAtomicLong atomicLong;

    private NormalOrderAntiFraudService antiFraudService;

    @BeforeEach
    void setUp() {
        antiFraudService = new NormalOrderAntiFraudService();
        ReflectionTestUtils.setField(antiFraudService, "redissonClient", redissonClient);
    }

    @Test
    void checkOrNullRejectsBlankArguments() {
        assertThat(antiFraudService.checkOrNull("", "g1")).isEqualTo("userId / goodsId 不能为空");
        verifyNoInteractions(redissonClient);
    }

    @Test
    void checkOrNullRejectsDuplicateSubmission() {
        when(redissonClient.<String>getBucket("mall:order:dedup:u1:g1")).thenReturn(dedupBucket);
        when(dedupBucket.setIfAbsent("1", Duration.ofSeconds(2))).thenReturn(false);

        assertThat(antiFraudService.checkOrNull("u1", "g1")).isEqualTo("提交过于频繁，请稍后再试");
        verify(redissonClient, never()).getAtomicLong(anyString());
    }

    @Test
    void checkOrNullExpiresCounterOnFirstRequest() {
        when(redissonClient.<String>getBucket("mall:order:dedup:u1:g1")).thenReturn(dedupBucket);
        when(redissonClient.getAtomicLong("mall:order:rate:min:u1")).thenReturn(atomicLong);
        when(dedupBucket.setIfAbsent("1", Duration.ofSeconds(2))).thenReturn(true);
        when(atomicLong.incrementAndGet()).thenReturn(1L);

        assertThat(antiFraudService.checkOrNull("u1", "g1")).isNull();
        verify(atomicLong).expire(Duration.ofMinutes(1));
    }

    @Test
    void checkOrNullRejectsRateLimitedRequest() {
        when(redissonClient.<String>getBucket("mall:order:dedup:u1:g1")).thenReturn(dedupBucket);
        when(redissonClient.getAtomicLong("mall:order:rate:min:u1")).thenReturn(atomicLong);
        when(dedupBucket.setIfAbsent("1", Duration.ofSeconds(2))).thenReturn(true);
        when(atomicLong.incrementAndGet()).thenReturn(51L);

        assertThat(antiFraudService.checkOrNull("u1", "g1")).isEqualTo("下单过于频繁，请稍后再试");
    }
}
