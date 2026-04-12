package cn.bugstack.test;

import cn.bugstack.infrastructure.adapter.port.PaySuccessRocketMqPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Date;

/**
 * RocketMQ 支付成功结算消息发送测试（与 {@link PaySuccessRocketMqPort#sendSettlementMessage} 一致）。
 * <p>
 * 使用 {@code test} 配置中的 {@code rocketmq.name-server}；需 NameServer/Broker 可达，且 topic
 * {@code rocketmq.topic.order_pay_success} 在 Broker 已存在或允许自动创建。
 * </p>
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("dev")
public class PaySuccessRocketMqSendTest {

    @Resource
    private PaySuccessRocketMqPort paySuccessRocketMqPort;

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    @Test
    public void test_sendSettlementMessage() {
        Assume.assumeNotNull(
                "RocketMQ 未自动装配（检查 rocketmq.name-server、rocketmq.producer.group 等）",
                rocketMQTemplate);

        String outTradeNo = "TEST_ROCKETMQ_" + System.currentTimeMillis();
        paySuccessRocketMqPort.sendSettlementMessage("10001", outTradeNo, new Date(),"1");
        log.info("PaySuccessRocketMqPort 已执行发送 outTradeNo={}", outTradeNo);
    }
}
