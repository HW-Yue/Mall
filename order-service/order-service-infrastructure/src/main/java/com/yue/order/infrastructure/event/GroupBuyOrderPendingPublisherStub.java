package com.yue.order.infrastructure.event;

import com.yue.order.domain.order.adapter.port.IGroupBuyOrderPendingPublisher;
import com.yue.order.types.enums.ResponseCode;
import com.yue.order.types.exception.AppException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * 未配置 RocketMQ 时占位
 */
@Service
@ConditionalOnMissingBean(GroupBuyOrderPendingPublisher.class)
public class GroupBuyOrderPendingPublisherStub implements IGroupBuyOrderPendingPublisher {

    @Override
    public void publishInsertSync(String messageBody) {
        throw new AppException(ResponseCode.UN_ERROR.getCode(), "未配置 rocketmq.name-server，无法入队");
    }
}
