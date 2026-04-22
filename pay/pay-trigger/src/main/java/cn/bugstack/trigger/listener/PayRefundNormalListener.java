package cn.bugstack.trigger.listener;

import cn.bugstack.domain.order.service.IOrderService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 消费普通订单退款消息，执行支付宝退款并更新 pay_order
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${rocketmq.topic.pay_refund_normal:pay-refund-normal}",
        consumerGroup = "${rocketmq.consumerGroup.payRefundNormal:CG_PAY_PAY_REFUND_NORMAL}"
)
public class PayRefundNormalListener implements RocketMQListener<String> {

    @Resource
    private IOrderService orderService;

    @Override
    public void onMessage(String message) {
        log.info("pay-service 收到普通订单退款消息: {}", message);
        try {
            JSONObject dto = JSON.parseObject(message);
            String outTradeNo = dto.getString("outTradeNo");
            if (StringUtils.isBlank(outTradeNo)) {
                log.error("消息缺少 outTradeNo: {}", message);
                return;
            }
            orderService.refundPayOrderByOutTradeNo(outTradeNo);
        } catch (AlipayApiException e) {
            log.error("pay-service 普通订单退款支付宝调用失败: {}", message, e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("pay-service 普通订单退款处理失败: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}
