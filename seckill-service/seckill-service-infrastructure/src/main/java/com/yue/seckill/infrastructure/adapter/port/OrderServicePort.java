package com.yue.seckill.infrastructure.adapter.port;

import com.yue.order.api.IOrderDubboService;
import com.yue.order.api.dto.QueryOrderByOutTradeNoRequestDTO;
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
    public String queryOrderIdByOutTradeNo(String userId, String outTradeNo) {
        QueryOrderByOutTradeNoRequestDTO request = new QueryOrderByOutTradeNoRequestDTO();
        request.setUserId(userId);
        request.setOutTradeNo(outTradeNo);
        try {
            var response = orderDubboService.queryOrderByOutTradeNo(request);
            if (response == null) {
                log.warn("order-service queryOrderByOutTradeNo 未查到订单 userId:{} outTradeNo:{}", userId, outTradeNo);
                return null;
            }
            return response.getOrderId();
        } catch (Exception e) {
            log.warn("order-service queryOrderByOutTradeNo 调用异常 userId:{} outTradeNo:{}", userId, outTradeNo, e);
            return null;
        }
    }

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
