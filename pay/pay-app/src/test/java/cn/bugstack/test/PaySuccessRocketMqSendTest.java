package cn.bugstack.test;

import cn.bugstack.domain.order.adapter.port.IPaySuccessPublisher;
import cn.bugstack.infrastructure.gateway.dto.SettlementMQMessageDTO;
import cn.bugstack.test.config.RocketMqMockTestConfig;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import jakarta.annotation.Resource;
import org.mockito.ArgumentCaptor;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * RocketMQ 支付成功结算消息发送测试。
 * <p>
 * 使用 {@code test-mock} profile，验证参数组装，不连接真实 MQ。
 * </p>
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.profiles.active=test-mock")
@Import(RocketMqMockTestConfig.class)
public class PaySuccessRocketMqSendTest {

    @Resource
    private IPaySuccessPublisher paySuccessPublisher;

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    @Test
    public void test_sendSettlementMessage() {
        Date outTradeTime = new Date(0L);
        String outTradeNo = "TEST_ROCKETMQ_" + System.currentTimeMillis();

        paySuccessPublisher.sendSettlementMessage("10001", outTradeNo, outTradeTime, "1");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rocketMQTemplate).convertAndSend(topicCaptor.capture(), bodyCaptor.capture());

        SettlementMQMessageDTO dto = JSON.parseObject(bodyCaptor.getValue(), SettlementMQMessageDTO.class);
        assertThat(topicCaptor.getValue()).isEqualTo("pay-success-normal");
        assertThat(dto.getUserId()).isEqualTo("10001");
        assertThat(dto.getOutTradeNo()).isEqualTo(outTradeNo);
        assertThat(dto.getOutTradeTime()).isEqualTo(outTradeTime);
        assertThat(dto.getMarketType()).isEqualTo("1");
        assertThat(dto.getSource()).isEqualTo("s01");
        assertThat(dto.getChannel()).isEqualTo("c01");
    }
}
