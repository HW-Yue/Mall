package com.yue.trigger.http;

import com.yue.api.dto.SkuStockRequestDTO;
import com.yue.api.response.Response;
import com.yue.infrastructure.feign.IOrderServiceForMallFeign;
import com.yue.order.api.dto.CreateOrderRequestDTO;
import com.yue.order.api.dto.CreateOrderResponseDTO;
import com.yue.trigger.service.order.NormalOrderAntiFraudService;
import com.yue.trigger.service.sku.SkuStockAppService;
import com.yue.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.math.BigDecimal;

/**
 * 普通商品 C 端下单：Redis 防刷 → 锁可售库存 → 订单服务异步落单
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mall/trade/")
public class OrderTradeController {

    @Resource
    private NormalOrderAntiFraudService normalOrderAntiFraudService;
    @Resource
    private SkuStockAppService skuStockAppService;
    @Resource
    private IOrderServiceForMallFeign orderServiceForMallFeign;

    @PostMapping("create_normal_order")
    public Response<CreateOrderResponseDTO> createNormalOrder(@RequestBody CreateOrderRequestDTO request) {
        log.info("create_normal_order req userId:{}", request != null ? request.getUserId() : null);
        if (request == null || StringUtils.isAnyBlank(request.getUserId(), request.getProductId())) {
            return Response.<CreateOrderResponseDTO>builder()
                    .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                    .info("userId / productId 不能为空")
                    .build();
        }
        if (request.getPayPrice() == null) {
            return Response.<CreateOrderResponseDTO>builder()
                    .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                    .info("payPrice 不能为空")
                    .build();
        }

        String anti = normalOrderAntiFraudService.checkOrNull(request.getUserId(), request.getProductId());
        if (anti != null) {
            return Response.<CreateOrderResponseDTO>builder()
                    .code(ResponseCode.RATE_LIMITER.getCode())
                    .info(anti)
                    .build();
        }

        request.setMarketType("normal");
        com.yue.api.response.Response<Boolean> lockResp = skuStockAppService.lockStock(
                new SkuStockRequestDTO(request.getProductId(), 1));
        if (lockResp == null || !ResponseCode.SUCCESS.getCode().equals(lockResp.getCode()) || !Boolean.TRUE.equals(lockResp.getData())) {
            String info = lockResp != null && lockResp.getInfo() != null ? lockResp.getInfo() : "锁库失败";
            return Response.<CreateOrderResponseDTO>builder()
                    .code(ResponseCode.E0009.getCode())
                    .info(info)
                    .build();
        }

        try {
            com.yue.order.api.response.Response<CreateOrderResponseDTO> orderResp = orderServiceForMallFeign.createOrderNormalFromMall(request);
            if (orderResp == null) {
                unlockQuietly(request.getProductId());
                return Response.<CreateOrderResponseDTO>builder()
                        .code(ResponseCode.HTTP_EXCEPTION.getCode())
                        .info("订单服务无响应")
                        .build();
            }
            if (!"0000".equals(orderResp.getCode()) || orderResp.getData() == null) {
                String info = orderResp.getInfo() != null ? orderResp.getInfo() : "创建订单失败";
                unlockQuietly(request.getProductId());
                return Response.<CreateOrderResponseDTO>builder()
                        .code(orderResp.getCode() != null ? orderResp.getCode() : ResponseCode.UN_ERROR.getCode())
                        .info(info)
                        .build();
            }
            CreateOrderResponseDTO data = orderResp.getData();
            if (StringUtils.isBlank(data.getOrderId())) {
                unlockQuietly(request.getProductId());
                return Response.<CreateOrderResponseDTO>builder()
                        .code(ResponseCode.UN_ERROR.getCode())
                        .info("订单服务返回无 orderId")
                        .build();
            }
            return Response.<CreateOrderResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(data)
                    .build();
        } catch (Exception e) {
            log.error("create_normal_order 调用 order-service 失败 userId:{}", request.getUserId(), e);
            unlockQuietly(request.getProductId());
            return Response.<CreateOrderResponseDTO>builder()
                    .code(ResponseCode.HTTP_EXCEPTION.getCode())
                    .info("创建订单失败: " + e.getMessage())
                    .build();
        }
    }

    private void unlockQuietly(String productId) {
        try {
            skuStockAppService.unlockStock(new SkuStockRequestDTO(productId, 1));
        } catch (Exception e) {
            log.warn("create_normal_order 回滚解锁失败 goodsId:{}", productId, e);
        }
    }
}
