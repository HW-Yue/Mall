package com.yue.seckill.infrastructure.adapter.port;

import com.yue.order.api.IOrderDubboService;
import com.yue.order.api.dto.RefundRequestDTO;
import com.yue.seckill.domain.trade.adapter.port.IOrderServicePort;
import com.yue.seckill.types.enums.ResponseCode;
import com.yue.seckill.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * 订单服务端口实现
 */
@Slf4j
@Service
public class OrderServicePort implements IOrderServicePort {

    @DubboReference
    private IOrderDubboService orderDubboService;

    @Override
    public void refundExecute(String userId, String orderId) {
        RefundRequestDTO request = RefundRequestDTO.builder()
                .userId(userId)
                .orderId(orderId)
                .build();
        try {
            orderDubboService.refundExecute(request);
        } catch (Exception e) {
            log.error("order-service refundExecute 失败 userId:{} orderId:{}", userId, orderId, e);
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "退款执行失败");
        }
    }

}
