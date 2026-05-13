package com.yue.seckill.test.domain;

import com.yue.seckill.domain.activity.adapter.repository.ISeckillActivityRepository;
import com.yue.seckill.domain.activity.model.entity.SeckillActivityEntity;
import com.yue.seckill.domain.activity.model.valobj.SkuVO;
import com.yue.seckill.domain.trade.adapter.port.IOrderServicePort;
import com.yue.seckill.domain.trade.adapter.port.ISeckillOrderTaskPort;
import com.yue.seckill.domain.trade.adapter.port.ISeckillStockPort;
import com.yue.seckill.domain.trade.model.entity.SeckillOrderResultEntity;
import com.yue.seckill.domain.trade.service.SeckillTradeServiceImpl;
import com.yue.seckill.types.enums.ResponseCode;
import com.yue.seckill.types.exception.AppException;
import com.yue.seckill.types.model.SeckillOrderTaskMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeckillTradeServiceImplTest {

    @Mock
    private ISeckillActivityRepository seckillActivityRepository;
    @Mock
    private ISeckillOrderTaskPort seckillOrderTaskPort;
    @Mock
    private IOrderServicePort orderServicePort;
    @Mock
    private ISeckillStockPort seckillStockPort;

    @InjectMocks
    private SeckillTradeServiceImpl seckillTradeService;

    @Test
    void createSeckillOrderReturnsMockTokenInTestMode() {
        SeckillOrderResultEntity result = seckillTradeService.createSeckillOrder(
                "u1", "g1", 1001L, "s01", "c01", "goods", "img", true);

        assertThat(result.getSeckillToken()).startsWith("test-");
        verifyNoInteractions(seckillActivityRepository, seckillOrderTaskPort, orderServicePort, seckillStockPort);
    }

    @Test
    void createSeckillOrderThrowsWhenActivityMissing() {
        when(seckillActivityRepository.querySeckillActivity(1001L)).thenReturn(null);

        assertThatThrownBy(() -> seckillTradeService.createSeckillOrder(
                "u1", "g1", 1001L, "s01", "c01", "goods", "img", false))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(ResponseCode.ACTIVITY_NOT_FOUND.getCode());
    }

    @Test
    void createSeckillOrderThrowsWhenActivityNotEffective() {
        when(seckillActivityRepository.querySeckillActivity(1001L))
                .thenReturn(SeckillActivityEntity.builder().activityId(1001L).status(0).build());

        assertThatThrownBy(() -> seckillTradeService.createSeckillOrder(
                "u1", "g1", 1001L, "s01", "c01", "goods", "img", false))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(ResponseCode.ACTIVITY_NOT_EFFECTIVE.getCode());
    }

    @Test
    void createSeckillOrderThrowsWhenStockInsufficient() {
        mockActiveActivity();
        when(seckillStockPort.deductByLua(1001L, "g1", "u1")).thenReturn(0);

        assertThatThrownBy(() -> seckillTradeService.createSeckillOrder(
                "u1", "g1", 1001L, "s01", "c01", "goods", "img", false))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(ResponseCode.STOCK_INSUFFICIENT.getCode());
    }

    @Test
    void createSeckillOrderThrowsWhenRepeatedRequest() {
        mockActiveActivity();
        when(seckillStockPort.deductByLua(1001L, "g1", "u1")).thenReturn(-1);

        assertThatThrownBy(() -> seckillTradeService.createSeckillOrder(
                "u1", "g1", 1001L, "s01", "c01", "goods", "img", false))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(ResponseCode.REPEATED_REQUEST.getCode());
    }

    @Test
    void createSeckillOrderBuildsMessageAndSavesTokenOnSuccess() {
        mockActiveActivityAndSku();
        when(seckillStockPort.deductByLua(1001L, "g1", "u1")).thenReturn(1);

        SeckillOrderResultEntity result = seckillTradeService.createSeckillOrder(
                "u1", "g1", 1001L, "s01", "c01", "", null, false);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SeckillOrderTaskMessage> messageCaptor = ArgumentCaptor.forClass(SeckillOrderTaskMessage.class);
        verify(seckillStockPort).saveSeckillToken(tokenCaptor.capture(), eq("u1"), eq("g1"), eq(1001L));
        verify(seckillOrderTaskPort).sendCreateOrderTask(messageCaptor.capture());

        String token = tokenCaptor.getValue();
        SeckillOrderTaskMessage message = messageCaptor.getValue();
        assertThat(result.getSeckillToken()).isEqualTo(token);
        assertThat(message.getSeckillToken()).isEqualTo(token);
        assertThat(message.getGoodsName()).isEqualTo("秒杀商品");
        assertThat(message.getGoodsImageUrl()).isEmpty();
        assertThat(message.getOriginalPrice()).isEqualByComparingTo("100");
        assertThat(message.getPayPrice()).isEqualByComparingTo("80");
        assertThat(message.getDeductionPrice()).isEqualByComparingTo("20");
    }

    @Test
    void createSeckillOrderRollsBackWhenSendTaskFails() {
        mockActiveActivityAndSku();
        when(seckillStockPort.deductByLua(1001L, "g1", "u1")).thenReturn(1);
        doThrow(new IllegalStateException("mq down")).when(seckillOrderTaskPort).sendCreateOrderTask(any());

        assertThatThrownBy(() -> seckillTradeService.createSeckillOrder(
                "u1", "g1", 1001L, "s01", "c01", "goods", "img", false))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(ResponseCode.CREATE_ORDER_FAILED.getCode());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(seckillStockPort).saveSeckillToken(tokenCaptor.capture(), eq("u1"), eq("g1"), eq(1001L));
        verify(seckillStockPort).rollbackSeckillOrder(eq(1001L), eq("g1"), eq("u1"), eq(tokenCaptor.getValue()));
    }

    private void mockActiveActivityAndSku() {
        mockActiveActivity();
        when(seckillActivityRepository.querySkuByGoodsId("g1"))
                .thenReturn(SkuVO.builder()
                        .goodsId("g1")
                        .originalPrice(new BigDecimal("100"))
                        .build());
    }

    private void mockActiveActivity() {
        when(seckillActivityRepository.querySeckillActivity(1001L))
                .thenReturn(SeckillActivityEntity.builder()
                        .activityId(1001L)
                        .status(1)
                        .seckillPrice(new BigDecimal("80"))
                        .build());
    }
}
