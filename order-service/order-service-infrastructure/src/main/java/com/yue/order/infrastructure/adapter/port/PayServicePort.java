package com.yue.order.infrastructure.adapter.port;

import com.alibaba.fastjson.JSON;
import com.yue.order.domain.order.adapter.port.IPayPort;
import com.yue.order.infrastructure.gateway.IPayService;
import com.yue.order.infrastructure.gateway.dto.CreatePayRequestDTO;
import com.yue.order.infrastructure.gateway.response.GatewayResponse;
import com.yue.order.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Slf4j
@Service
public class PayServicePort implements IPayPort {

    @Resource
    private IPayService payService;

    @Override
    public String getPayUrl(String userId, String outTradeNo, String goodsId, String goodsName,
                            BigDecimal originalPrice, BigDecimal deductionPrice,
                            BigDecimal payPrice, String marketType) {
        CreatePayRequestDTO requestDTO = CreatePayRequestDTO.builder()
                .userId(userId)
                .outTradeNo(outTradeNo)
                .productId(goodsId)
                .productName(goodsName)
                .originalPrice(originalPrice)
                .deductionPrice(deductionPrice)
                .payPrice(payPrice)
                .marketType(marketType)
                .build();

        log.info("调支付服务 userId:{} outTradeNo:{} req:{}", userId, outTradeNo, JSON.toJSONString(requestDTO));
        GatewayResponse<String> response = payService.createPayOrder(requestDTO);
        log.info("支付服务响应 userId:{} outTradeNo:{} resp:{}", userId, outTradeNo, JSON.toJSONString(response));

        if (response == null || !"0000".equals(response.getCode())) {
            String errInfo = response != null ? response.getInfo() : "null response";
            throw new AppException("PAY_FAILED", "获取支付链接失败: " + errInfo);
        }
        return response.getData();
    }

    @Override
    public void refund(String outTradeNo, BigDecimal payPrice, String reason) {
        // TODO: 待 pay-service 暴露退款 HTTP 接口后实现
        // pay-service domain 层退款逻辑已写完，HTTP trigger 层接口尚未实现（见 CLAUDE.md）
        log.info("退款请求 outTradeNo:{} payPrice:{} reason:{} - pay-service 退款接口待实现", outTradeNo, payPrice, reason);
    }
}
