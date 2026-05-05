package cn.bugstack.test.infrastructure;

import cn.bugstack.domain.order.adapter.repository.IOrderRepository;
import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import cn.bugstack.infrastructure.adapter.port.*;
import cn.bugstack.infrastructure.dao.IUserDao;
import cn.bugstack.infrastructure.dao.po.UserAccount;
import cn.bugstack.infrastructure.gateway.IWeixinApiService;
import cn.bugstack.infrastructure.gateway.dto.WeixinQrCodeResponseDTO;
import cn.bugstack.infrastructure.gateway.dto.WeixinTokenResponseDTO;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayInfrastructureTest {

    @Mock
    private RocketMQTemplate rocketMQTemplate;
    @Mock
    private IOrderRepository orderRepository;
    @Mock
    private IWeixinApiService weixinApiService;
    @Mock
    private IUserDao userDao;
    @Mock
    private Call<WeixinTokenResponseDTO> tokenCall;
    @Mock
    private Call<WeixinQrCodeResponseDTO> qrCodeCall;

    private Cache<String, String> accessTokenCache;

    @BeforeEach
    void setUp() {
        accessTokenCache = CacheBuilder.newBuilder().build();
    }

    @Test
    void mqProducersRouteByMarketType() {
        PaySuccessMqProducer successProducer = new PaySuccessMqProducer();
        ReflectionTestUtils.setField(successProducer, "rocketMQTemplate", rocketMQTemplate);
        ReflectionTestUtils.setField(successProducer, "paySuccessNormalTopic", "pay-success-normal");
        ReflectionTestUtils.setField(successProducer, "paySuccessGroupBuyTopic", "pay-success-group-buy");
        ReflectionTestUtils.setField(successProducer, "paySuccessSeckillTopic", "pay-success-seckill");
        ReflectionTestUtils.setField(successProducer, "source", "s01");
        ReflectionTestUtils.setField(successProducer, "channel", "c01");

        successProducer.sendSettlementMessage("u1", "OID-1", new java.util.Date(0), "group_buy");

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rocketMQTemplate).convertAndSend(eq("pay-success-group-buy"), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).contains("\"outTradeNo\":\"OID-1\"", "\"source\":\"s01\"", "\"channel\":\"c01\"");

        OrderCloseMqProducer closeProducer = new OrderCloseMqProducer();
        ReflectionTestUtils.setField(closeProducer, "rocketMQTemplate", rocketMQTemplate);
        ReflectionTestUtils.setField(closeProducer, "orderCloseNormalTopic", "close-normal");
        ReflectionTestUtils.setField(closeProducer, "orderCloseGroupBuyTopic", "close-group");
        ReflectionTestUtils.setField(closeProducer, "orderCloseSeckillTopic", "close-seckill");
        ReflectionTestUtils.setField(closeProducer, "source", "s01");
        ReflectionTestUtils.setField(closeProducer, "channel", "c01");
        closeProducer.sendOrderCloseMessage("u2", "OID-2", "seckill");
        verify(rocketMQTemplate).convertAndSend(eq("close-seckill"), contains("\"outTradeNo\":\"OID-2\""));

        OrderRefundMqProducer refundProducer = new OrderRefundMqProducer();
        ReflectionTestUtils.setField(refundProducer, "rocketMQTemplate", rocketMQTemplate);
        ReflectionTestUtils.setField(refundProducer, "payRefundNormalTopic", "refund-normal");
        ReflectionTestUtils.setField(refundProducer, "payRefundGroupBuyTopic", "refund-group");
        ReflectionTestUtils.setField(refundProducer, "payRefundSeckillTopic", "refund-seckill");
        ReflectionTestUtils.setField(refundProducer, "source", "s01");
        ReflectionTestUtils.setField(refundProducer, "channel", "c01");
        refundProducer.sendPayRefundMessage("u3", "OID-3", "normal");
        verify(rocketMQTemplate).convertAndSend(eq("refund-normal"), contains("\"outTradeNo\":\"OID-3\""));
    }

    @Test
    void payRefundReceiptMqProducerSendsTransactionMessage() {
        PayRefundReceiptMqProducer producer = new PayRefundReceiptMqProducer();
        ReflectionTestUtils.setField(producer, "rocketMQTemplate", rocketMQTemplate);
        ReflectionTestUtils.setField(producer, "payRefundNormalResultTopic", "refund-normal-result");
        ReflectionTestUtils.setField(producer, "payRefundGroupBuyResultTopic", "refund-group-result");
        ReflectionTestUtils.setField(producer, "payRefundSeckillResultTopic", "refund-seckill-result");
        TransactionSendResult txResult = mock(TransactionSendResult.class);
        when(txResult.getLocalTransactionState()).thenReturn(LocalTransactionState.COMMIT_MESSAGE);
        when(rocketMQTemplate.sendMessageInTransaction(anyString(), any(Message.class), any()))
                .thenReturn(txResult);

        producer.publishRefundReceipt("u1", "OID-4", "group_buy");

        verify(rocketMQTemplate).sendMessageInTransaction(eq("refund-group-result"), any(Message.class), any(Map.class));
    }

    @Test
    void refundTransactionListenerCommitsOrRollsBackByOrderStatus() {
        PayRefundReceiptTransactionListener listener = new PayRefundReceiptTransactionListener();
        ReflectionTestUtils.setField(listener, "repository", orderRepository);
        Message<String> message = MessageBuilder.withPayload("payload")
                .setHeader("userId", "u1")
                .setHeader("outTradeNo", "OID-5")
                .build();

        when(orderRepository.queryOrderByUserIdAndOrderId("u1", "OID-5"))
                .thenReturn(OrderEntity.builder().orderStatusVO(OrderStatusVO.WAIT_REFUND).build());
        when(orderRepository.refundOrder("u1", "OID-5")).thenReturn(true);
        assertThat(listener.executeLocalTransaction(message, null)).isEqualTo(RocketMQLocalTransactionState.COMMIT);

        when(orderRepository.queryOrderByUserIdAndOrderId("u1", "OID-5"))
                .thenReturn(OrderEntity.builder().orderStatusVO(OrderStatusVO.CLOSE).build());
        assertThat(listener.checkLocalTransaction(message)).isEqualTo(RocketMQLocalTransactionState.ROLLBACK);
    }

    @Test
    void loginPortReadsAndCachesWeixinAccessToken() throws Exception {
        LoginPort loginPort = new LoginPort();
        ReflectionTestUtils.setField(loginPort, "appid", "app");
        ReflectionTestUtils.setField(loginPort, "appSecret", "secret");
        ReflectionTestUtils.setField(loginPort, "template_id", "tpl");
        ReflectionTestUtils.setField(loginPort, "weixinAccessToken", accessTokenCache);
        ReflectionTestUtils.setField(loginPort, "weixinApiService", weixinApiService);
        ReflectionTestUtils.setField(loginPort, "userDao", userDao);

        WeixinTokenResponseDTO tokenResponse = new WeixinTokenResponseDTO();
        ReflectionTestUtils.setField(tokenResponse, "access_token", "token-1");
        WeixinQrCodeResponseDTO qrCodeResponse = new WeixinQrCodeResponseDTO();
        ReflectionTestUtils.setField(qrCodeResponse, "ticket", "ticket-1");

        when(weixinApiService.getToken("client_credential", "app", "secret")).thenReturn(tokenCall);
        when(tokenCall.execute()).thenReturn(Response.success(tokenResponse));
        when(weixinApiService.createQrCode(eq("token-1"), any())).thenReturn(qrCodeCall);
        when(qrCodeCall.execute()).thenReturn(Response.success(qrCodeResponse));

        assertThat(loginPort.createQrCodeTicket("scene-a")).isEqualTo("ticket-1");
        assertThat(accessTokenCache.getIfPresent("app")).isEqualTo("token-1");

        when(userDao.queryByOpenid("openid-1")).thenReturn(UserAccount.builder().username("tester").build());
        assertThat(loginPort.queryUsernameByOpenid("openid-1")).isEqualTo("tester");
    }

    @Test
    void loginPortRejectsDuplicateUsername() {
        LoginPort loginPort = new LoginPort();
        ReflectionTestUtils.setField(loginPort, "userDao", userDao);
        when(userDao.queryByUsername("dup")).thenReturn(UserAccount.builder().username("dup").build());

        assertThatThrownBy(() -> loginPort.registerUser("openid", "dup", "pwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户名已存在");
    }
}
