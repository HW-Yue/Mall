package com.yue.order.infrastructure.event;

import com.yue.order.domain.order.adapter.repository.IOrderRepository;
import com.yue.order.domain.order.model.entity.OrderEntity;
import com.yue.order.domain.order.model.valobj.OrderStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Map;

/**
 * 订单事务消息监听器。
 * 通过 bizType 区分发货任务和退款请求，统一处理本地事务与回查。
 */
@Slf4j
@Service
@RocketMQTransactionListener(rocketMQTemplateBeanName = "rocketMQTemplate")
public class OrderShipTaskTransactionListener implements RocketMQLocalTransactionListener {

    private static final String BIZ_TYPE_ORDER_SHIP_TASK = "order_ship_task";
    private static final String BIZ_TYPE_PAY_REFUND_REQUEST = "pay_refund_request";

    @Resource
    private IOrderRepository orderRepository;

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        return handleLocalTransaction(msg, arg, true);
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        return handleLocalTransaction(msg, null, false);
    }

    private RocketMQLocalTransactionState handleLocalTransaction(Message msg, Object arg, boolean executePhase) {
        String bizType = firstNonBlank(header(msg, "bizType"), payloadString(msg, "bizType"), argString(arg, "bizType"));
        if (StringUtils.isBlank(bizType)) {
            bizType = BIZ_TYPE_ORDER_SHIP_TASK;
        }

        if (BIZ_TYPE_PAY_REFUND_REQUEST.equals(bizType)) {
            return handleRefundTransaction(msg, arg, executePhase);
        }
        return handleShipTaskTransaction(msg, arg, executePhase);
    }

    private RocketMQLocalTransactionState handleShipTaskTransaction(Message msg, Object arg, boolean executePhase) {
        String userId = firstNonBlank(header(msg, "userId"), payloadString(msg, "userId"), argString(arg, "userId"));
        String orderId = firstNonBlank(header(msg, "orderId"), payloadString(msg, "orderId"), argString(arg, "orderId"));
        String outTradeNo = firstNonBlank(header(msg, "outTradeNo"), payloadString(msg, "outTradeNo"), argString(arg, "outTradeNo"));

        if (StringUtils.isAnyBlank(userId, orderId, outTradeNo)) {
            log.error("order-ship-task 事务消息缺少必填字段 headers:{} payload:{}",
                    msg != null ? msg.getHeaders() : null, msg != null ? msg.getPayload() : null);
            return RocketMQLocalTransactionState.ROLLBACK;
        }

        OrderEntity order = orderRepository.queryByUserIdAndOrderId(userId, orderId);
        if (order == null) {
            log.warn("order-ship-task 本地事务未找到订单 userId:{} orderId:{} outTradeNo:{}", userId, orderId, outTradeNo);
            return RocketMQLocalTransactionState.ROLLBACK;
        }

        OrderStatusVO status = order.getStatus();
        if (status == OrderStatusVO.CLOSE
                || status == OrderStatusVO.WAIT_REFUND
                || status == OrderStatusVO.REFUNDED) {
            log.warn("order-ship-task 订单已关闭/退款，回滚事务 userId:{} orderId:{} status:{}",
                    userId, orderId, status.getCode());
            return RocketMQLocalTransactionState.ROLLBACK;
        }

        if (status == OrderStatusVO.WAIT_SHIP
                || status == OrderStatusVO.SHIPPED
                || status == OrderStatusVO.DELIVERED) {
            log.info("order-ship-task 订单已处于履约阶段，直接提交事务 userId:{} orderId:{} status:{}",
                    userId, orderId, status.getCode());
            return RocketMQLocalTransactionState.COMMIT;
        }

        if (status == OrderStatusVO.PAY_SUCCESS) {
            int updated = orderRepository.updateWaitShipByOrderId(userId, orderId);
            if (updated == 1) {
                log.info("order-ship-task 本地事务写入待发货成功 userId:{} orderId:{} outTradeNo:{}",
                        userId, orderId, outTradeNo);
                return RocketMQLocalTransactionState.COMMIT;
            }

            OrderEntity refreshed = orderRepository.queryByUserIdAndOrderId(userId, orderId);
            if (refreshed != null) {
                OrderStatusVO refreshedStatus = refreshed.getStatus();
                if (refreshedStatus == OrderStatusVO.WAIT_SHIP
                        || refreshedStatus == OrderStatusVO.SHIPPED
                        || refreshedStatus == OrderStatusVO.DELIVERED) {
                    log.info("order-ship-task 本地事务幂等提交 userId:{} orderId:{} status:{}",
                            userId, orderId, refreshedStatus.getCode());
                    return RocketMQLocalTransactionState.COMMIT;
                }
                if (refreshedStatus == OrderStatusVO.CLOSE
                        || refreshedStatus == OrderStatusVO.WAIT_REFUND
                        || refreshedStatus == OrderStatusVO.REFUNDED) {
                    return RocketMQLocalTransactionState.ROLLBACK;
                }
            }

            log.warn("order-ship-task 本地事务更新结果不确定，等待 broker 回查 userId:{} orderId:{}",
                    userId, orderId);
            return RocketMQLocalTransactionState.UNKNOWN;
        }

        if (status == OrderStatusVO.LOCK) {
            log.warn("order-ship-task 订单尚未支付完成，等待 broker 回查 userId:{} orderId:{}",
                    userId, orderId);
            return RocketMQLocalTransactionState.UNKNOWN;
        }

        log.warn("order-ship-task 未命中预期状态，等待 broker 回查 userId:{} orderId:{} status:{} executePhase:{}",
                userId, orderId, status != null ? status.getCode() : null, executePhase);
        return RocketMQLocalTransactionState.UNKNOWN;
    }

    private RocketMQLocalTransactionState handleRefundTransaction(Message msg, Object arg, boolean executePhase) {
        String userId = firstNonBlank(header(msg, "userId"), payloadString(msg, "userId"), argString(arg, "userId"));
        String outTradeNo = firstNonBlank(header(msg, "outTradeNo"), payloadString(msg, "outTradeNo"), argString(arg, "outTradeNo"));

        if (StringUtils.isAnyBlank(userId, outTradeNo)) {
            log.error("pay-refund-request 事务消息缺少必填字段 headers:{} payload:{}",
                    msg != null ? msg.getHeaders() : null, msg != null ? msg.getPayload() : null);
            return RocketMQLocalTransactionState.ROLLBACK;
        }

        OrderEntity order = orderRepository.queryByOutTradeNo(outTradeNo);
        if (order == null) {
            log.warn("pay-refund-request 本地事务未找到订单 userId:{} outTradeNo:{}", userId, outTradeNo);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
        if (!StringUtils.equals(userId, order.getUserId())) {
            log.warn("pay-refund-request 用户不匹配 userId:{} orderUserId:{} outTradeNo:{}",
                    userId, order.getUserId(), outTradeNo);
            return RocketMQLocalTransactionState.ROLLBACK;
        }

        OrderStatusVO status = order.getStatus();
        if (status == OrderStatusVO.REFUNDED || status == OrderStatusVO.WAIT_REFUND) {
            log.info("pay-refund-request 订单已处于退款链路，直接提交 userId:{} outTradeNo:{} status:{}",
                    userId, outTradeNo, status.getCode());
            return RocketMQLocalTransactionState.COMMIT;
        }
        if (status == OrderStatusVO.CLOSE) {
            log.warn("pay-refund-request 订单已关闭，回滚事务 userId:{} outTradeNo:{}", userId, outTradeNo);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
        if (status == OrderStatusVO.LOCK) {
            log.warn("pay-refund-request 订单尚未支付，回滚事务 userId:{} outTradeNo:{}", userId, outTradeNo);
            return RocketMQLocalTransactionState.ROLLBACK;
        }

        orderRepository.updateWaitRefundByOutTradeNo(outTradeNo);
        OrderEntity refreshed = orderRepository.queryByOutTradeNo(outTradeNo);
        if (refreshed != null) {
            OrderStatusVO refreshedStatus = refreshed.getStatus();
            if (refreshedStatus == OrderStatusVO.WAIT_REFUND) {
                log.info("pay-refund-request 本地事务写入待退款成功 userId:{} outTradeNo:{} status:{}",
                        userId, outTradeNo, refreshedStatus.getCode());
                return RocketMQLocalTransactionState.COMMIT;
            }
            if (refreshedStatus == OrderStatusVO.REFUNDED) {
                log.info("pay-refund-request 本地事务幂等提交 userId:{} outTradeNo:{} status:{}",
                        userId, outTradeNo, refreshedStatus.getCode());
                return RocketMQLocalTransactionState.COMMIT;
            }
            if (refreshedStatus == OrderStatusVO.CLOSE || refreshedStatus == OrderStatusVO.LOCK) {
                return RocketMQLocalTransactionState.ROLLBACK;
            }
        }

        log.warn("pay-refund-request 本地事务更新结果不确定，等待 broker 回查 userId:{} outTradeNo:{}",
                userId, outTradeNo);
        return RocketMQLocalTransactionState.UNKNOWN;
    }

    private String header(Message msg, String key) {
        if (msg == null || msg.getHeaders() == null) {
            return null;
        }
        Object value = msg.getHeaders().get(key);
        return value != null ? value.toString() : null;
    }

    private String payloadString(Message msg, String key) {
        if (msg == null || msg.getPayload() == null) {
            return null;
        }
        Object payload = msg.getPayload();
        if (payload instanceof Map<?, ?> map) {
            Object value = map.get(key);
            return value != null ? value.toString() : null;
        }
        return null;
    }

    private String argString(Object arg, String key) {
        if (arg instanceof Map<?, ?> map) {
            Object value = map.get(key);
            return value != null ? value.toString() : null;
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
