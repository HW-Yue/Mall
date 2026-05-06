package com.yue.groupbuy.test.infrastructure;

import com.alibaba.fastjson.JSON;
import com.yue.groupbuy.infrastructure.adapter.port.OrderServicePort;
import com.yue.groupbuy.infrastructure.adapter.port.TradePort;
import com.yue.groupbuy.infrastructure.dao.ITOrderGroupDao;
import com.yue.groupbuy.infrastructure.dao.po.TOrderGroup;
import com.yue.groupbuy.infrastructure.event.GroupBuyEventPublisher;
import com.yue.groupbuy.infrastructure.event.GroupBuyRefundMqProducer;
import com.yue.groupbuy.infrastructure.event.GroupBuyTimeoutRefundProducer;
import com.yue.groupbuy.infrastructure.event.dto.TeamSuccessNotifyMessage;
import com.yue.order.api.IOrderDubboService;
import com.yue.order.api.dto.CreateOrderResponseDTO;
import com.yue.order.api.dto.QueryOrderByOutTradeNoRequestDTO;
import com.yue.groupbuy.domain.trade.model.entity.NotifyTaskEntity;
import com.yue.groupbuy.domain.trade.model.valobj.NotifyTypeEnumVO;
import com.yue.groupbuy.types.enums.NotifyTaskHTTPEnumVO;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupBuyInfrastructureTest {

    @Mock
    private RocketMQTemplate rocketMQTemplate;
    @Mock
    private IOrderDubboService orderDubboService;
    @Mock
    private ITOrderGroupDao orderGroupDao;

    @Test
    void eventPublisherBuildsSuccessMessage() {
        GroupBuyEventPublisher publisher = new GroupBuyEventPublisher();
        ReflectionTestUtils.setField(publisher, "rocketMQTemplate", rocketMQTemplate);
        ReflectionTestUtils.setField(publisher, "groupBuySuccessNotifyTopic", "group-buy-success");

        publisher.publishGroupBuySuccess("T1", List.of(Map.of(
                "orderId", "OID-1",
                "userId", "u1",
                "goodsType", "normal",
                "resKey", "k",
                "resValue", "v",
                "payPrice", new BigDecimal("88"))));

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rocketMQTemplate).convertAndSend(eq("group-buy-success"), bodyCaptor.capture());
        TeamSuccessNotifyMessage message = JSON.parseObject(bodyCaptor.getValue(), TeamSuccessNotifyMessage.class);
        assertThat(message.getTeamId()).isEqualTo("T1");
        assertThat(message.getOrders()).hasSize(1);
    }

    @Test
    void refundAndTimeoutProducersUseExpectedTopics() {
        GroupBuyRefundMqProducer refundProducer = new GroupBuyRefundMqProducer();
        ReflectionTestUtils.setField(refundProducer, "rocketMQTemplate", rocketMQTemplate);
        ReflectionTestUtils.setField(refundProducer, "orderCloseGroupBuyTopic", "order-close-group");
        ReflectionTestUtils.setField(refundProducer, "payRefundGroupBuyTopic", "pay-refund-group");
        refundProducer.sendOrderCloseMessage("OUT-1", "u1");
        refundProducer.sendPayRefundMessage("OUT-2", "u2");
        verify(rocketMQTemplate).convertAndSend(eq("order-close-group"), contains("\"outTradeNo\":\"OUT-1\""));
        verify(rocketMQTemplate).convertAndSend(eq("pay-refund-group"), contains("\"outTradeNo\":\"OUT-2\""));

        GroupBuyTimeoutRefundProducer timeoutProducer = new GroupBuyTimeoutRefundProducer();
        ReflectionTestUtils.setField(timeoutProducer, "rocketMQTemplate", rocketMQTemplate);
        ReflectionTestUtils.setField(timeoutProducer, "groupBuyTimeoutRefundTopic", "timeout-topic");
        timeoutProducer.sendTimeoutRefundMessage("T2", 12345L);
        verify(rocketMQTemplate).syncSendDeliverTimeMills(eq("timeout-topic"), contains("\"teamId\":\"T2\""), eq(12345L));
    }

    @Test
    void orderServicePortFallsBackToQueryByOutTradeNoAndPersistsOrderGroup() {
        OrderServicePort port = new OrderServicePort();
        ReflectionTestUtils.setField(port, "orderDubboService", orderDubboService);
        ReflectionTestUtils.setField(port, "tOrderGroupDao", orderGroupDao);
        // createOrder 返回 null orderId 触发 fallback
        when(orderDubboService.createOrder(any())).thenReturn(CreateOrderResponseDTO.builder().build());
        CreateOrderResponseDTO queryDto = CreateOrderResponseDTO.builder().orderId("OID-9").build();
        when(orderDubboService.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequestDTO.class))).thenReturn(queryDto);

        String orderId = port.createOrder("u1", "g1", "goods", "img",
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"),
                "s01", "c01", "OUT-9", "T9", 1L, new java.util.Date(0), new java.util.Date(1));

        assertThat(orderId).isEqualTo("OID-9");
        verify(orderGroupDao).insert(any(TOrderGroup.class));
    }

    @Test
    void tradePortRoutesMqAndHttpNotifications() throws Exception {
        GroupBuyEventPublisher publisher = mock(GroupBuyEventPublisher.class);
        TradePort port = new TradePort();
        ReflectionTestUtils.setField(port, "groupBuyEventPublisher", publisher);

        String mqResult = port.groupBuyNotify(NotifyTaskEntity.builder()
                .teamId("T1")
                .notifyType(NotifyTypeEnumVO.MQ.getCode())
                .notifyMQ("topic-1")
                .parameterJson("{\"ok\":1}")
                .build());
        assertThat(mqResult).isEqualTo(NotifyTaskHTTPEnumVO.SUCCESS.getCode());
        verify(publisher).publishRawMessage("topic-1", "{\"ok\":1}");

        String httpResult = port.groupBuyNotify(NotifyTaskEntity.builder()
                .teamId("T2")
                .notifyType(NotifyTypeEnumVO.HTTP.getCode())
                .notifyUrl("http://example.com")
                .build());
        assertThat(httpResult).isEqualTo(NotifyTaskHTTPEnumVO.SUCCESS.getCode());
    }
}
