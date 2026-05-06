package com.yue.order.test.infrastructure;

import com.yue.order.domain.order.adapter.repository.IOrderRepository;
import com.yue.order.domain.order.model.entity.OrderEntity;
import com.yue.order.domain.order.model.valobj.OrderStatusVO;
import cn.bugstack.api.IPayDubboService;
import com.yue.order.infrastructure.adapter.port.PayServicePort;
import com.yue.order.infrastructure.event.*;
import com.yue.order.types.exception.AppException;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderInfrastructureTest {

    @Mock
    private RocketMQTemplate rocketMQTemplate;
    @Mock
    private IPayDubboService payDubboService;
    @Mock
    private IOrderRepository orderRepository;

    @Test
    void payServicePortBuildsRequestAndReturnsUrl() {
        PayServicePort port = new PayServicePort();
        ReflectionTestUtils.setField(port, "payDubboService", payDubboService);
        when(payDubboService.createPayOrder(any())).thenReturn("pay-url");

        String result = port.getPayUrl("u1", "OUT-1", "g1", "goods",
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"), "normal");

        assertThat(result).isEqualTo("pay-url");
    }

    @Test
    void payServicePortThrowsForFailedResponse() {
        PayServicePort port = new PayServicePort();
        ReflectionTestUtils.setField(port, "payDubboService", payDubboService);
        when(payDubboService.createPayOrder(any())).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> port.getPayUrl("u1", "OUT-1", "g1", "goods",
                new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("100"), "normal"))
                .isInstanceOf(AppException.class);
    }

    @Test
    void normalOrderPendingPublisherChecksSendStatus() {
        NormalOrderPendingPublisher publisher = new NormalOrderPendingPublisher();
        ReflectionTestUtils.setField(publisher, "rocketMQTemplate", rocketMQTemplate);
        ReflectionTestUtils.setField(publisher, "topic", "normal-order-create");
        SendResult sendResult = mock(SendResult.class);
        when(sendResult.getSendStatus()).thenReturn(SendStatus.SEND_OK);
        when(sendResult.getMsgId()).thenReturn("msg-1");
        when(rocketMQTemplate.syncSend("normal-order-create", "{\"a\":1}", 3000L)).thenReturn(sendResult);

        publisher.publishInsertSync("{\"a\":1}");

        verify(rocketMQTemplate).syncSend("normal-order-create", "{\"a\":1}", 3000L);
        assertThatThrownBy(() -> new NormalOrderPendingPublisherStub().publishInsertSync("x"))
                .isInstanceOf(AppException.class);
    }

    @Test
    void mqProducersPublishToResolvedTopics() {
        OrderPaidMqProducer paidProducer = new OrderPaidMqProducer();
        ReflectionTestUtils.setField(paidProducer, "rocketMQTemplate", rocketMQTemplate);
        ReflectionTestUtils.setField(paidProducer, "orderPaidNormalTopic", "paid-normal");
        ReflectionTestUtils.setField(paidProducer, "orderPaidGroupBuyTopic", "paid-group");
        ReflectionTestUtils.setField(paidProducer, "orderPaidSeckillTopic", "paid-seckill");
        paidProducer.publishOrderPaid("u1", "OID-1", "OUT-1", "group_buy", new java.util.Date(0));
        verify(rocketMQTemplate).convertAndSend(eq("paid-group"), contains("\"outTradeNo\":\"OUT-1\""));

        OrderCloseMqProducer closeProducer = new OrderCloseMqProducer();
        ReflectionTestUtils.setField(closeProducer, "rocketMQTemplate", rocketMQTemplate);
        ReflectionTestUtils.setField(closeProducer, "orderCloseNormalTopic", "close-normal");
        ReflectionTestUtils.setField(closeProducer, "orderCloseGroupBuyTopic", "close-group");
        ReflectionTestUtils.setField(closeProducer, "orderCloseSeckillTopic", "close-seckill");
        closeProducer.publishOrderClose("u1", "OID-1", "OUT-2", "seckill");
        verify(rocketMQTemplate).convertAndSend(eq("close-seckill"), contains("\"outTradeNo\":\"OUT-2\""));
    }

    @Test
    void transactionProducersSendTransactionMessages() {
        TransactionSendResult txResult = mock(TransactionSendResult.class);
        when(txResult.getLocalTransactionState()).thenReturn(LocalTransactionState.COMMIT_MESSAGE);
        when(rocketMQTemplate.sendMessageInTransaction(anyString(), any(Message.class), any()))
                .thenReturn(txResult);

        OrderRefundMqProducer refundProducer = new OrderRefundMqProducer();
        ReflectionTestUtils.setField(refundProducer, "rocketMQTemplate", rocketMQTemplate);
        ReflectionTestUtils.setField(refundProducer, "payRefundNormalTopic", "refund-normal");
        ReflectionTestUtils.setField(refundProducer, "payRefundGroupBuyTopic", "refund-group");
        ReflectionTestUtils.setField(refundProducer, "payRefundSeckillTopic", "refund-seckill");
        refundProducer.publishPayRefund("u1", "OUT-3", "normal");
        verify(rocketMQTemplate).sendMessageInTransaction(eq("refund-normal"), any(Message.class), any(Map.class));

        OrderShipTaskMqProducer shipProducer = new OrderShipTaskMqProducer();
        ReflectionTestUtils.setField(shipProducer, "rocketMQTemplate", rocketMQTemplate);
        ReflectionTestUtils.setField(shipProducer, "orderShipTaskTopic", "ship-task");
        shipProducer.publishOrderShipTask("u1", "OID-2", "OUT-4");
        verify(rocketMQTemplate).sendMessageInTransaction(eq("ship-task"), any(Message.class), any(Map.class));
    }

    @Test
    void orderShipTaskTransactionListenerHandlesCommitRollbackAndUnknown() {
        OrderShipTaskTransactionListener listener = new OrderShipTaskTransactionListener();
        ReflectionTestUtils.setField(listener, "orderRepository", orderRepository);

        when(orderRepository.queryByUserIdAndOrderId("u1", "OID-1"))
                .thenReturn(OrderEntity.builder().status(OrderStatusVO.PAY_SUCCESS).build());
        when(orderRepository.updateWaitShipByOrderId("u1", "OID-1")).thenReturn(1);
        Message<Map<String, Object>> shipMessage = org.springframework.messaging.support.MessageBuilder.<Map<String, Object>>withPayload(
                        Map.of("bizType", (Object) "order_ship_task", "userId", "u1", "orderId", "OID-1", "outTradeNo", "OUT-1"))
                .setHeader("bizType", "order_ship_task")
                .setHeader("userId", "u1")
                .setHeader("orderId", "OID-1")
                .setHeader("outTradeNo", "OUT-1")
                .build();
        assertThat(listener.executeLocalTransaction(shipMessage, shipMessage.getPayload()))
                .isEqualTo(RocketMQLocalTransactionState.COMMIT);

        when(orderRepository.queryByOutTradeNo("OUT-2"))
                .thenReturn(OrderEntity.builder().userId("u2").status(OrderStatusVO.CLOSE).build());
        Message<Map<String, Object>> refundMessage = org.springframework.messaging.support.MessageBuilder.<Map<String, Object>>withPayload(
                        Map.of("bizType", (Object) "pay_refund_request", "userId", "u2", "outTradeNo", "OUT-2"))
                .setHeader("bizType", "pay_refund_request")
                .setHeader("userId", "u2")
                .setHeader("outTradeNo", "OUT-2")
                .build();
        assertThat(listener.checkLocalTransaction(refundMessage))
                .isEqualTo(RocketMQLocalTransactionState.ROLLBACK);
    }
}
