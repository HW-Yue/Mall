package com.yue.seckill.test.activity;

import com.yue.seckill.api.dto.CreateSeckillOrderRequestDTO;
import com.yue.seckill.api.dto.PreheatRequestDTO;
import com.yue.seckill.api.response.Response;
import com.yue.seckill.domain.activity.adapter.port.ISeckillGoodsCachePort;
import com.yue.seckill.domain.activity.adapter.repository.ISeckillActivityRepository;
import com.yue.seckill.domain.activity.model.entity.SeckillGoodsEntity;
import com.yue.seckill.domain.activity.model.valobj.SeckillActivityWithGoodsVO;
import com.yue.seckill.domain.activity.model.valobj.SeckillStockVO;
import com.yue.seckill.domain.activity.service.SeckillAdminServiceImpl;
import com.yue.seckill.domain.activity.service.SeckillMarketServiceImpl;
import com.yue.seckill.domain.trade.adapter.port.ISeckillStockPort;
import com.yue.seckill.domain.trade.model.entity.SeckillOrderResultEntity;
import com.yue.seckill.domain.trade.service.ISeckillTradeService;
import com.yue.seckill.trigger.http.SeckillAdminController;
import com.yue.seckill.trigger.http.SeckillMarketController;
import com.yue.seckill.trigger.http.SeckillTradeController;
import com.yue.seckill.types.enums.ResponseCode;
import com.yue.seckill.types.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeckillActivityAndControllerTest {

    @Mock
    private ISeckillActivityRepository activityRepository;
    @Mock
    private ISeckillGoodsCachePort goodsCachePort;
    @Mock
    private ISeckillStockPort seckillStockPort;
    @Mock
    private ISeckillTradeService seckillTradeService;

    private SeckillMarketServiceImpl marketService;
    private SeckillAdminServiceImpl adminService;
    private SeckillMarketController marketController;
    private SeckillAdminController adminController;
    private SeckillTradeController tradeController;

    @BeforeEach
    void setUp() {
        marketService = new SeckillMarketServiceImpl();
        ReflectionTestUtils.setField(marketService, "seckillActivityRepository", activityRepository);
        ReflectionTestUtils.setField(marketService, "seckillGoodsCachePort", goodsCachePort);

        adminService = new SeckillAdminServiceImpl();
        ReflectionTestUtils.setField(adminService, "seckillActivityRepository", activityRepository);
        ReflectionTestUtils.setField(adminService, "seckillStockPort", seckillStockPort);

        marketController = new SeckillMarketController();
        ReflectionTestUtils.setField(marketController, "seckillMarketService", marketService);

        adminController = new SeckillAdminController();
        ReflectionTestUtils.setField(adminController, "seckillAdminService", adminService);

        tradeController = new SeckillTradeController();
        ReflectionTestUtils.setField(tradeController, "seckillTradeService", seckillTradeService);
    }

    @Test
    void marketServiceLoadsGoodsThroughCachePort() {
        SeckillGoodsEntity goods = SeckillGoodsEntity.builder().goodsId("g1").goodsName("phone").build();
        when(goodsCachePort.getGoodsList(any())).thenAnswer(invocation -> {
            Supplier<List<SeckillGoodsEntity>> loader = invocation.getArgument(0);
            return loader.get();
        });
        when(activityRepository.querySeckillGoodsList()).thenReturn(List.of(goods));

        List<SeckillGoodsEntity> result = marketService.querySeckillGoodsList();

        assertThat(result).containsExactly(goods);
        verify(activityRepository).querySeckillGoodsList();
    }

    @Test
    void adminServicePreheatsWithExplicitOrDbStock() {
        adminService.preheatStock(1001L, "g1", 9, 60L);
        verify(seckillStockPort).preloadStock(1001L, "g1", 9, 60L);

        reset(seckillStockPort);
        when(activityRepository.querySeckillStockList()).thenReturn(List.of(
                SeckillStockVO.builder().activityId(1002L).goodsId("g2").remainCount(7).build()));

        adminService.preheatStock(1002L, "g2", null, 120L);

        verify(seckillStockPort).preloadStock(1002L, "g2", 7, 120L);
        when(seckillStockPort.getStockValue(1002L, "g2")).thenReturn("7");
        assertThat(adminService.getPreheatStatus(1002L, "g2")).isEqualTo("7");
    }

    @Test
    void marketControllerMapsGoodsList() {
        when(goodsCachePort.getGoodsList(any())).thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
        when(activityRepository.querySeckillGoodsList()).thenReturn(List.of(
                SeckillGoodsEntity.builder()
                        .goodsId("g1").goodsName("phone").goodsImageUrl("img")
                        .originalPrice(new BigDecimal("100")).payPrice(new BigDecimal("80"))
                        .source("s01").channel("c01").activityId(1001L).build()));

        Response<?> response = marketController.querySeckillGoodsList();

        assertThat(response.getCode()).isEqualTo(ResponseCode.SUCCESS.getCode());
        assertThat(response.getData()).extracting("seckillGoodsList").asList().hasSize(1);
    }

    @Test
    void adminControllerQueriesActivitiesAndPreheatsAll() {
        when(activityRepository.querySeckillActivitiesWithGoods()).thenReturn(List.of(
                SeckillActivityWithGoodsVO.builder()
                        .activityId(1001L)
                        .activityName("秒杀")
                        .seckillPrice(new BigDecimal("80"))
                        .remainCount(5)
                        .goodsList(List.of(
                                SeckillActivityWithGoodsVO.GoodsItem.builder().goodsId("g1").goodsName("phone").build(),
                                SeckillActivityWithGoodsVO.GoodsItem.builder().goodsId("g2").goodsName("pad").build()))
                        .build()));
        when(seckillStockPort.getStockValue(1001L, "g1")).thenReturn("5");
        when(seckillStockPort.getStockValue(1001L, "g2")).thenReturn(null);

        Response<?> queryResponse = adminController.queryActivities();
        assertThat(queryResponse.getCode()).isEqualTo(ResponseCode.SUCCESS.getCode());
        assertThat(queryResponse.getData()).extracting("activities").asList().hasSize(1);

        PreheatRequestDTO request = new PreheatRequestDTO();
        request.setActivityId(1001L);
        request.setGoodsId("all");
        request.setStock(3);
        request.setExpireSeconds(30L);

        Response<?> preheatResponse = adminController.preheat(request);

        assertThat(preheatResponse.getCode()).isEqualTo(ResponseCode.SUCCESS.getCode());
        assertThat(preheatResponse.getData()).extracting("successCount").isEqualTo(2);
        verify(seckillStockPort).preloadStock(1001L, "g1", 3, 30L);
        verify(seckillStockPort).preloadStock(1001L, "g2", 3, 30L);
    }

    @Test
    void adminControllerRejectsIllegalSinglePreheatRequest() {
        PreheatRequestDTO request = new PreheatRequestDTO();
        request.setActivityId(1001L);

        Response<?> response = adminController.preheat(request);

        assertThat(response.getCode()).isEqualTo(ResponseCode.ILLEGAL_PARAMETER.getCode());
        assertThat(response.getInfo()).contains("goodsId");
    }

    @Test
    void tradeControllerCreatesOrderAndRefunds() {
        CreateSeckillOrderRequestDTO request = new CreateSeckillOrderRequestDTO();
        request.setUserId("u1");
        request.setProductId("g1");
        request.setActivityId(1001L);
        request.setSource("s01");
        request.setChannel("c01");
        when(seckillTradeService.createSeckillOrder(eq("u1"), eq("g1"), eq(1001L), eq("s01"), eq("c01"), any(), any(), eq(false)))
                .thenReturn(SeckillOrderResultEntity.builder().seckillToken("tk").orderId("OID-1").build());

        Response<?> createResponse = tradeController.createPayOrder(request);
        assertThat(createResponse.getCode()).isEqualTo(ResponseCode.SUCCESS.getCode());
        assertThat(createResponse.getData()).extracting("seckillToken").isEqualTo("tk");

        Response<Boolean> refundResponse = tradeController.refund("{\"userId\":\"u1\",\"orderId\":\"OID-1\"}");
        assertThat(refundResponse.getCode()).isEqualTo(ResponseCode.SUCCESS.getCode());
        assertThat(refundResponse.getData()).isTrue();
        verify(seckillTradeService).refund("u1", "OID-1");
    }

    @Test
    void tradeControllerHandlesValidationAndBusinessFailure() {
        CreateSeckillOrderRequestDTO request = new CreateSeckillOrderRequestDTO();
        request.setUserId("u1");

        Response<?> invalidCreate = tradeController.createPayOrder(request);
        assertThat(invalidCreate.getCode()).isEqualTo(ResponseCode.ILLEGAL_PARAMETER.getCode());

        request.setProductId("g1");
        request.setActivityId(1001L);
        doThrow(new AppException(ResponseCode.STOCK_INSUFFICIENT)).when(seckillTradeService)
                .createSeckillOrder(anyString(), anyString(), anyLong(), any(), any(), any(), any(), anyBoolean());
        Response<?> bizError = tradeController.createPayOrder(request);
        assertThat(bizError.getCode()).isEqualTo(ResponseCode.STOCK_INSUFFICIENT.getCode());

        Response<Boolean> invalidRefund = tradeController.refund("{\"userId\":\"u1\"}");
        assertThat(invalidRefund.getCode()).isEqualTo(ResponseCode.ILLEGAL_PARAMETER.getCode());
    }
}
