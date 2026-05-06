package com.yue.order.infrastructure.adapter.port;

import cn.bugstack.api.IPayDubboService;
import cn.bugstack.api.dto.CreatePayRequestDTO;
import com.alibaba.fastjson.JSON;
import com.yue.order.domain.order.adapter.port.IPayPort;
import com.yue.order.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class PayServicePort implements IPayPort {

    @DubboReference
    private IPayDubboService payDubboService;

    @Override
    public String getPayUrl(String userId, String outTradeNo, String goodsId, String goodsName,
                            BigDecimal originalPrice, BigDecimal deductionPrice,
                            BigDecimal payPrice, String marketType) {
        CreatePayRequestDTO requestDTO = new CreatePayRequestDTO();
        requestDTO.setUserId(userId);
        requestDTO.setOutTradeNo(outTradeNo);
        requestDTO.setProductId(goodsId);
        requestDTO.setProductName(goodsName);
        requestDTO.setOriginalPrice(originalPrice);
        requestDTO.setDeductionPrice(deductionPrice);
        requestDTO.setPayPrice(payPrice);
        requestDTO.setMarketType(marketType);

        log.info("调支付服务 userId:{} outTradeNo:{} req:{}", userId, outTradeNo, JSON.toJSONString(requestDTO));
        try {
            String payUrl = payDubboService.createPayOrder(requestDTO);
            log.info("支付服务响应 userId:{} outTradeNo:{} payUrl:{}", userId, outTradeNo, payUrl);
            return payUrl;
        } catch (Exception e) {
            log.error("支付服务调用失败 userId:{} outTradeNo:{}", userId, outTradeNo, e);
            throw new AppException("PAY_FAILED", "获取支付链接失败: " + e.getMessage());
        }
    }

    @Override
    public void refund(String outTradeNo, BigDecimal payPrice, String reason) {
        // TODO: 待 pay-service 暴露退款 HTTP 接口后实现
        // pay-service domain 层退款逻辑已写完，HTTP trigger 层接口尚未实现（见 CLAUDE.md）
        log.info("退款请求 outTradeNo:{} payPrice:{} reason:{} - pay-service 退款接口待实现", outTradeNo, payPrice, reason);
    }
}
