package com.yue.groupbuy.test.trigger;

import com.yue.groupbuy.api.dto.*;
import com.yue.groupbuy.api.response.Response;
import com.yue.groupbuy.domain.activity.model.entity.GroupBuyGoodsEntity;
import com.yue.groupbuy.domain.activity.model.entity.TrialBalanceEntity;
import com.yue.groupbuy.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.yue.groupbuy.domain.activity.model.entity.MarketProductEntity;
import com.yue.groupbuy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.yue.groupbuy.domain.activity.model.valobj.TeamStatisticVO;
import com.yue.groupbuy.domain.activity.service.IIndexGroupBuyMarketService;
import com.yue.groupbuy.domain.trade.model.entity.LockOrderCommand;
import com.yue.groupbuy.domain.trade.model.entity.MarketPayOrderEntity;
import com.yue.groupbuy.domain.trade.service.IGroupBuyDomainService;
import com.yue.groupbuy.infrastructure.config.AgentRuntimeProperties;
import com.yue.groupbuy.trigger.http.GroupBuyMarketController;
import com.yue.groupbuy.trigger.http.GroupBuyTradeController;
import com.yue.groupbuy.types.enums.ResponseCode;
import com.yue.groupbuy.types.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupBuyControllerTest {

    @Mock
    private IIndexGroupBuyMarketService marketService;
    @Mock
    private IGroupBuyDomainService domainService;

    private GroupBuyMarketController marketController;
    private GroupBuyTradeController tradeController;

    @BeforeEach
    void setUp() {
        marketController = new GroupBuyMarketController();
        tradeController = new GroupBuyTradeController();
        ReflectionTestUtils.setField(marketController, "indexGroupBuyMarketService", marketService);
        AgentRuntimeProperties properties = new AgentRuntimeProperties();
        properties.getFeatures().setStatisticsReportEnabled(true);
        ReflectionTestUtils.setField(marketController, "agentRuntimeProperties", properties);
        ReflectionTestUtils.setField(tradeController, "groupBuyDomainService", domainService);
    }

    @Test
    void marketControllerReturnsGoodsList() {
        when(marketService.queryGroupBuyGoodsList())
                .thenReturn(List.of(GroupBuyGoodsEntity.builder().goodsId("g1").goodsName("goods").payPrice(new BigDecimal("88")).build()));

        Response<QueryGroupBuyGoodsListResponseDTO> response = marketController.queryGroupBuyGoodsList();

        assertThat(response.getCode()).isEqualTo(ResponseCode.SUCCESS.getCode());
        assertThat(response.getData().getGroupBuyGoodsList()).hasSize(1);
    }

    @Test
    void marketControllerAssemblesMarketConfig() throws Exception {
        when(marketService.indexMarketTrial(any(MarketProductEntity.class)))
                .thenReturn(TrialBalanceEntity.builder()
                        .goodsId("g1")
                        .goodsImageUrl("img")
                        .originalPrice(new BigDecimal("100"))
                        .deductionPrice(new BigDecimal("10"))
                        .payPrice(new BigDecimal("90"))
                        .groupBuyActivityDiscountVO(GroupBuyActivityDiscountVO.builder().activityId(1L).build())
                        .build());
        when(marketService.queryInProgressUserGroupBuyOrderDetailList("g1", "u1", 1, 2))
                .thenReturn(List.of(UserGroupBuyOrderDetailEntity.builder()
                        .userId("u2").teamId("T1").activityId(1L).targetCount(3).completeCount(1).lockCount(1)
                        .validStartTime(new Date(0)).validEndTime(new Date(System.currentTimeMillis() + 60_000)).outTradeNo("OUT-1")
                        .build()));
        when(marketService.queryTeamStatisticByGoodsId("g1"))
                .thenReturn(TeamStatisticVO.builder().allTeamCount(2).allTeamCompleteCount(1).allTeamUserCount(4).build());

        GoodsMarketRequestDTO request = new GoodsMarketRequestDTO();
        request.setUserId("u1");
        request.setSource("s01");
        request.setChannel("c01");
        request.setGoodsId("g1");
        Response<GoodsMarketResponseDTO> response = marketController.queryGroupBuyMarketConfig(request);

        assertThat(response.getCode()).isEqualTo(ResponseCode.SUCCESS.getCode());
        assertThat(response.getData().getActivityId()).isEqualTo(1L);
        assertThat(response.getData().getTeamList()).hasSize(1);
    }

    @Test
    void tradeControllerValidatesAndReturnsResult() {
        when(domainService.createGroupBuyOrder(any(LockOrderCommand.class)))
                .thenReturn(MarketPayOrderEntity.builder()
                        .orderId("OID-1").teamId("T1").originalPrice(new BigDecimal("100"))
                        .deductionPrice(new BigDecimal("10")).payPrice(new BigDecimal("90")).build());

        CreateGroupBuyOrderRequestDTO request = new CreateGroupBuyOrderRequestDTO();
        request.setUserId("u1");
        request.setProductId("g1");
        request.setActivityId(1L);
        Response<CreateGroupBuyOrderResponseDTO> response = tradeController.createPayOrder(request);

        assertThat(response.getCode()).isEqualTo(ResponseCode.SUCCESS.getCode());
        assertThat(response.getData().getOrderId()).isEqualTo("OID-1");
    }

    @Test
    void tradeControllerMapsBusinessExceptions() {
        doThrow(new AppException("E001", "boom")).when(domainService).refundGroupBuyOrder("u1", "OID-2");

        GroupBuyRefundRequestDTO request = new GroupBuyRefundRequestDTO();
        request.setUserId("u1");
        request.setOrderId("OID-2");
        Response<Boolean> response = tradeController.refund(request);

        assertThat(response.getCode()).isEqualTo("E001");
        assertThat(response.getData()).isFalse();
    }
}
