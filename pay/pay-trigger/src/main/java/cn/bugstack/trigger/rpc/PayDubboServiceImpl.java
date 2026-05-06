package cn.bugstack.trigger.rpc;

import cn.bugstack.api.IPayDubboService;
import cn.bugstack.api.dto.CreatePayRequestDTO;
import cn.bugstack.domain.order.model.entity.CreateOrderEntity;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.valobj.MarketTypeVO;
import cn.bugstack.domain.order.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import jakarta.annotation.Resource;

@Slf4j
@DubboService
public class PayDubboServiceImpl implements IPayDubboService {

    @Resource
    private IOrderService orderService;

    @Override
    public String createPayOrder(CreatePayRequestDTO request) {
        log.info("dubbo createPayOrder userId:{} outTradeNo:{}", request.getUserId(), request.getOutTradeNo());
        CreateOrderEntity createOrderEntity = CreateOrderEntity.builder()
                .originalPrice(request.getOriginalPrice())
                .deductionPrice(request.getDeductionPrice())
                .payPrice(request.getPayPrice())
                .orderId(request.getOutTradeNo())
                .teamId(request.getTeamId())
                .productId(request.getProductId())
                .marketTypeVO(MarketTypeVO.fromCode(request.getMarketType()))
                .userId(request.getUserId())
                .productName(request.getProductName())
                .build();
        try {
            PayOrderEntity payOrderEntity = orderService.createOrder(createOrderEntity);
            log.info("dubbo createPayOrder success userId:{} payUrl:{}", request.getUserId(), payOrderEntity.getPayUrl());
            return payOrderEntity.getPayUrl();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("createPayOrder failed: " + e.getMessage(), e);
        }
    }
}
