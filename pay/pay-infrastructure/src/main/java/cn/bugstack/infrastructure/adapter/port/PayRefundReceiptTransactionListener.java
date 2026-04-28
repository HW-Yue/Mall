package cn.bugstack.infrastructure.adapter.port;

import cn.bugstack.domain.order.adapter.repository.IOrderRepository;
import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 退款完成回执事务消息监听器
 */
@Slf4j
@Service
@RocketMQTransactionListener(rocketMQTemplateBeanName = "rocketMQTemplate")
public class PayRefundReceiptTransactionListener implements RocketMQLocalTransactionListener {

    @Resource
    private IOrderRepository repository;

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        return handleTransaction(msg);
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        return handleTransaction(msg);
    }

    private RocketMQLocalTransactionState handleTransaction(Message msg) {
        String userId = header(msg, "userId");
        String orderId = header(msg, "outTradeNo");
        if (StringUtils.isAnyBlank(userId, orderId)) {
            log.error("退款回执事务消息缺少必填字段 headers:{}", msg != null ? msg.getHeaders() : null);
            return RocketMQLocalTransactionState.ROLLBACK;
        }

        OrderEntity orderEntity = repository.queryOrderByUserIdAndOrderId(userId, orderId);
        if (orderEntity == null) {
            log.warn("退款回执事务消息订单不存在 userId:{} orderId:{}", userId, orderId);
            return RocketMQLocalTransactionState.ROLLBACK;
        }

        OrderStatusVO status = orderEntity.getOrderStatusVO();
        if (OrderStatusVO.REFUNDED.getCode().equals(status != null ? status.getCode() : null)) {
            log.info("退款回执事务消息订单已退款完成，直接提交 userId:{} orderId:{}", userId, orderId);
            return RocketMQLocalTransactionState.COMMIT;
        }

        if (status == OrderStatusVO.WAIT_REFUND || status == OrderStatusVO.PAY_SUCCESS || status == OrderStatusVO.PAY_AFTER_CLOSE) {
            boolean updated = repository.refundOrder(userId, orderId);
            if (updated) {
                log.info("退款回执本地事务写入完成 userId:{} orderId:{} status:{}", userId, orderId, status.getCode());
                return RocketMQLocalTransactionState.COMMIT;
            }
            OrderEntity refreshed = repository.queryOrderByUserIdAndOrderId(userId, orderId);
            if (refreshed != null && OrderStatusVO.REFUNDED.getCode().equals(refreshed.getOrderStatusVO() != null ? refreshed.getOrderStatusVO().getCode() : null)) {
                return RocketMQLocalTransactionState.COMMIT;
            }
            return RocketMQLocalTransactionState.UNKNOWN;
        }

        if (status == OrderStatusVO.CLOSE) {
            log.warn("退款回执事务消息订单已关闭，回滚 userId:{} orderId:{}", userId, orderId);
            return RocketMQLocalTransactionState.ROLLBACK;
        }

        return RocketMQLocalTransactionState.UNKNOWN;
    }

    private String header(Message msg, String key) {
        if (msg == null || msg.getHeaders() == null) {
            return null;
        }
        Object value = msg.getHeaders().get(key);
        return value != null ? value.toString() : null;
    }
}
