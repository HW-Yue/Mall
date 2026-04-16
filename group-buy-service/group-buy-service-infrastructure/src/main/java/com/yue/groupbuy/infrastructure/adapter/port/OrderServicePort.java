package com.yue.groupbuy.infrastructure.adapter.port;

import com.yue.groupbuy.domain.trade.adapter.port.IOrderServicePort;
import com.yue.groupbuy.infrastructure.dao.ITOrderGroupDao;
import com.yue.groupbuy.infrastructure.dao.po.TOrderGroup;
import com.yue.groupbuy.infrastructure.gateway.IOrderService;
import com.yue.groupbuy.infrastructure.gateway.dto.CreateOrderRequestDTO;
import com.yue.groupbuy.infrastructure.gateway.dto.CreateOrderResponseDTO;
import com.yue.groupbuy.infrastructure.gateway.dto.GatewayResponse;
import com.yue.groupbuy.infrastructure.gateway.dto.RefundRequestDTO;
import com.yue.groupbuy.types.enums.ResponseCode;
import com.yue.groupbuy.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Service
public class OrderServicePort implements IOrderServicePort {

    @Resource
    private IOrderService orderService;
    @Resource
    private ITOrderGroupDao tOrderGroupDao;

    @Override
    public String createOrder(String userId, String productId, String goodsName, String goodsImageUrl,
                              BigDecimal originalPrice, BigDecimal deductionPrice, BigDecimal payPrice,
                              String source, String channel, String outTradeNo,
                              String teamId, Long activityId, Date startTime, Date endTime) {
        CreateOrderRequestDTO request = CreateOrderRequestDTO.builder()
                .userId(userId)
                .productId(productId)
                .goodsName(goodsName)
                .goodsImageUrl(goodsImageUrl)
                .marketType("group_buy")
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

        String orderId = response.getData().getOrderId();
        tOrderGroupDao.insert(TOrderGroup.builder()
                .orderId(orderId)
                .userId(userId)
                .teamId(teamId)
                .activityId(activityId)
                .goodsId(productId)
                .outTradeNo(outTradeNo)
                .originalPrice(originalPrice)
                .deductionPrice(deductionPrice)
                .payPrice(payPrice)
                .startTime(startTime)
                .endTime(endTime)
                .build());
        return orderId;
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
            throw new AppException("退款执行失败");
        }
    }
}
