package com.yue.order.trigger.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yue.order.domain.order.model.entity.OrderEntity;
import com.yue.order.domain.order.model.valobj.MarketTypeVO;
import com.yue.order.domain.order.model.valobj.OrderStatusVO;
import com.yue.order.domain.order.adapter.repository.IOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.math.BigDecimal;

/**
 * 普通商品异步落库（mall 已锁库存，此处仅 INSERT）
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${app.rocketmq.topic.normalOrderCreate:normal-order-create}",
        consumerGroup = "${app.rocketmq.consumerGroup.normalOrderCreate:CG_NORMAL_ORDER_CREATE}"
)
public class NormalOrderCreateListener implements RocketMQListener<String> {

    @Resource
    private IOrderRepository orderRepository;

    @Override
    public void onMessage(String message) {
        log.info("normal-order-create 收到消息: {}", message);
        try {
            JSONObject dto = JSON.parseObject(message);
            String orderId = dto.getString("orderId");
            String userId = dto.getString("userId");
            if (StringUtils.isAnyBlank(orderId, userId)) {
                log.error("normal-order-create 缺少 orderId/userId: {}", message);
                return;
            }
            if (orderRepository.queryByUserIdAndOrderId(userId, orderId) != null) {
                log.info("normal-order-create 幂等跳过 orderId:{}", orderId);
                return;
            }

            String outTradeNo = dto.getString("outTradeNo");
            BigDecimal payPrice = dto.getBigDecimal("payPrice");
            if (StringUtils.isBlank(outTradeNo) || payPrice == null) {
                log.error("normal-order-create 缺少 outTradeNo/payPrice: {}", message);
                return;
            }

            BigDecimal deduction = dto.getBigDecimal("deductionPrice");
            OrderEntity order = OrderEntity.builder()
                    .orderId(orderId)
                    .userId(userId)
                    .goodsId(dto.getString("goodsId"))
                    .goodsName(dto.getString("goodsName"))
                    .goodsImageUrl(dto.getString("goodsImageUrl"))
                    .source(dto.getString("source"))
                    .channel(dto.getString("channel"))
                    .originalPrice(dto.getBigDecimal("originalPrice"))
                    .deductionPrice(deduction != null ? deduction : BigDecimal.ZERO)
                    .payPrice(payPrice)
                    .status(OrderStatusVO.LOCK)
                    .outTradeNo(outTradeNo)
                    .marketType(MarketTypeVO.NORMAL)
                    .notifyType(StringUtils.isNotBlank(dto.getString("notifyType")) ? dto.getString("notifyType") : "MQ")
                    .build();

            orderRepository.insertOrderWithoutMallLock(order);
            log.info("normal-order-create 落库成功 orderId:{}", orderId);
        } catch (Exception e) {
            log.error("normal-order-create 处理失败: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}
