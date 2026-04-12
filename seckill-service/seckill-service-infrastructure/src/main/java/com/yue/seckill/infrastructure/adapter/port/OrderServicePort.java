package com.yue.seckill.infrastructure.adapter.port;

import com.yue.seckill.domain.trade.adapter.port.IOrderServicePort;
import com.yue.seckill.infrastructure.gateway.IOrderService;
import com.yue.seckill.infrastructure.gateway.dto.CreateOrderRequestDTO;
import com.yue.seckill.infrastructure.gateway.dto.CreateOrderResponseDTO;
import com.yue.seckill.infrastructure.gateway.dto.GatewayResponse;
import com.yue.seckill.infrastructure.gateway.dto.RefundRequestDTO;
import com.yue.seckill.types.enums.ResponseCode;
import com.yue.seckill.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 订单服务端口实现
 */
@Slf4j
@Service
public class OrderServicePort implements IOrderServicePort {

    @Resource
    private IOrderService orderService;

    @Override
    public String createOrder(String userId, String productId, String goodsName, String goodsImageUrl,
                              BigDecimal originalPrice, BigDecimal deductionPrice, BigDecimal payPrice,
                              String source, String channel, String outTradeNo) {
        CreateOrderRequestDTO request = CreateOrderRequestDTO.builder()
                .userId(userId)
                .productId(productId)
                .goodsName(goodsName)
                .goodsImageUrl(goodsImageUrl)
                .marketType("seckill")
                .originalPrice(originalPrice)
                .deductionPrice(deductionPrice)
                .payPrice(payPrice)
                .source(source)
                .channel(channel)
                .outTradeNo(outTradeNo)
                .build();

        GatewayResponse<CreateOrderResponseDTO> response = orderService.createOrder(request);
        if (response == null || !ResponseCode.SUCCESS.getCode().equals(response.getCode())) {
            log.error("order-service createOrder 失败: {}", response);
            throw new AppException(ResponseCode.CREATE_ORDER_FAILED);
        }

        return response.getData().getOrderId();
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
