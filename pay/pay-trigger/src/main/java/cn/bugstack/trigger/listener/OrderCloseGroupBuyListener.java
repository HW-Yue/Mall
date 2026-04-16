package cn.bugstack.trigger.listener;

import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
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
 * 消费拼团未支付关单消息，关闭 pay_order。
 * 若 pay_order 已支付，则执行退款而非关单。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${rocketmq.topic.order_close_group_buy:order-close-group-buy}",
        consumerGroup = "${rocketmq.consumerGroup.orderCloseGroupBuy:CG_PAY_ORDER_CLOSE_GROUP_BUY}"
)
public class OrderCloseGroupBuyListener implements RocketMQListener<String> {

    @Resource
    private IOrderService orderService;

    @Override
    public void onMessage(String message) {
        log.info("pay-service 收到拼团关单消息: {}", message);
        try {
            JSONObject dto = JSON.parseObject(message);
            String outTradeNo = dto.getString("outTradeNo");
            if (StringUtils.isBlank(outTradeNo)) {
                log.error("消息缺少 outTradeNo: {}", message);
                return;
            }

            OrderEntity payOrder = orderService.queryOrderByOrderId(outTradeNo);
            if (payOrder == null) {
                log.warn("pay-service 未找到支付单，无需处理 outTradeNo:{}", outTradeNo);
                return;
            }

            String status = payOrder.getOrderStatusVO() != null ? payOrder.getOrderStatusVO().getCode() : null;
            if (OrderStatusVO.PAY_WAIT.getCode().equals(status)) {
                orderService.closePayOrder(outTradeNo);
            } else if (OrderStatusVO.PAY_SUCCESS.getCode().equals(status)) {
                log.warn("pay-service 发现订单已支付但收到关单消息，执行退款 outTradeNo:{}", outTradeNo);
                orderService.refundPayOrderByOutTradeNo(outTradeNo);
            } else {
                log.info("pay-service 支付单状态无需处理 outTradeNo:{} status:{}", outTradeNo, status);
            }
        } catch (AlipayApiException e) {
            log.error("pay-service 拼团关单/退款支付宝调用失败: {}", message, e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("pay-service 拼团关单处理失败: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}
