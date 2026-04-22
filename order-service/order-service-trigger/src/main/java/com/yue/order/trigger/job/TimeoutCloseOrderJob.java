package com.yue.order.trigger.job;

import com.yue.order.domain.order.service.IOrderDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 订单服务主关单入口：扫描超时未支付订单并触发关单
 */
@Slf4j
@Component
public class TimeoutCloseOrderJob {

    @Resource
    private IOrderDomainService orderDomainService;

    @Scheduled(cron = "0 0/30 * * * ?")
    public void exec() {
        try {
            log.info("businessTimeoutClose 开始：order-service 扫描超时未支付订单");
            orderDomainService.triggerTimeoutClose();
        } catch (Exception e) {
            log.error("businessTimeoutClose 失败：order-service 扫描超时未支付订单异常", e);
        }
    }
}
