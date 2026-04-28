package com.yue.seckill.infrastructure.adapter.port;

import com.yue.seckill.domain.trade.adapter.port.IOrderServicePort;
import com.yue.seckill.infrastructure.gateway.IOrderService;
import com.yue.seckill.infrastructure.gateway.dto.CreateOrderResponseDTO;
import com.yue.seckill.infrastructure.gateway.dto.GatewayResponse;
import com.yue.seckill.infrastructure.gateway.dto.QueryOrderByOutTradeNoRequestDTO;
import com.yue.seckill.infrastructure.gateway.dto.RefundRequestDTO;
import com.yue.seckill.types.enums.ResponseCode;
import com.yue.seckill.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 订单服务端口实现
 */
@Slf4j
@Service
public class OrderServicePort implements IOrderServicePort {

    @Resource
    private IOrderService orderService;

    @Override
    public String queryOrderIdByOutTradeNo(String userId, String outTradeNo) {
        QueryOrderByOutTradeNoRequestDTO request = new QueryOrderByOutTradeNoRequestDTO();
        request.setUserId(userId);
        request.setOutTradeNo(outTradeNo);
        try {
            GatewayResponse<CreateOrderResponseDTO> response = orderService.queryOrderByOutTradeNo(request);
            if (response == null || !ResponseCode.SUCCESS.getCode().equals(response.getCode()) || response.getData() == null) {
                log.warn("order-service queryOrderByOutTradeNo 未查到订单 userId:{} outTradeNo:{} resp:{}",
                        userId, outTradeNo, response);
                return null;
            }
            return response.getData().getOrderId();
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

        GatewayResponse<Boolean> response = orderService.refundExecute(request);
        if (response == null || !ResponseCode.SUCCESS.getCode().equals(response.getCode())) {
            log.error("order-service refundExecute 失败: {}", response);
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "退款执行失败");
        }
    }

}
