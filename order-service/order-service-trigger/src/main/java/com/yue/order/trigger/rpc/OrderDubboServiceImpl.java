package com.yue.order.trigger.rpc;

import com.yue.order.api.IOrderDubboService;
import com.yue.order.api.dto.CreateOrderRequestDTO;
import com.yue.order.api.dto.CreateOrderResponseDTO;
import com.yue.order.api.dto.RefundRequestDTO;
import com.yue.order.domain.order.model.entity.CreateOrderCommand;
import com.yue.order.domain.order.service.IOrderDomainService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import jakarta.annotation.Resource;

@Slf4j
@DubboService
public class OrderDubboServiceImpl implements IOrderDubboService {

    @Resource
    private IOrderDomainService orderDomainService;

    @Override
    public CreateOrderResponseDTO createOrder(CreateOrderRequestDTO request) {
        log.info("dubbo createOrder userId:{}", request.getUserId());
        CreateOrderCommand command = toCommand(request);
        String orderId = orderDomainService.createOrder(command);
        return CreateOrderResponseDTO.builder()
                .orderId(orderId)
                .build();
    }

    @Override
    public CreateOrderResponseDTO createOrderNormalFromMall(CreateOrderRequestDTO request) {
        log.info("dubbo createOrderNormalFromMall userId:{}", request.getUserId());
        CreateOrderCommand command = CreateOrderCommand.builder()
                .userId(request.getUserId())
                .goodsId(request.getProductId())
                .marketType("normal")
                .originalPrice(request.getOriginalPrice())
                .deductionPrice(request.getDeductionPrice())
                .payPrice(request.getPayPrice())
                .source(request.getSource())
                .channel(request.getChannel())
                .goodsName(request.getGoodsName())
                .goodsImageUrl(request.getGoodsImageUrl())
                .build();
        var result = orderDomainService.submitNormalOrderFromMall(command);
        return CreateOrderResponseDTO.builder()
                .orderId(result.getOrderId())
                .build();
    }

    @Override
    public Boolean refundExecute(RefundRequestDTO request) {
        log.info("dubbo refundExecute userId:{} orderId:{}", request.getUserId(), request.getOrderId());
        orderDomainService.refundExecute(request.getUserId(), request.getOrderId());
        return true;
    }

    private CreateOrderCommand toCommand(CreateOrderRequestDTO request) {
        return CreateOrderCommand.builder()
                .userId(request.getUserId())
                .goodsId(request.getProductId())
                .marketType(request.getMarketType())
                .originalPrice(request.getOriginalPrice())
                .deductionPrice(request.getDeductionPrice())
                .payPrice(request.getPayPrice())
                .source(request.getSource())
                .channel(request.getChannel())
                .goodsName(request.getGoodsName())
                .goodsImageUrl(request.getGoodsImageUrl())
                .build();
    }
}
