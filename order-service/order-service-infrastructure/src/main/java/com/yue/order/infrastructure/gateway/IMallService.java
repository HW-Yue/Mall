package com.yue.order.infrastructure.gateway;

import com.yue.order.infrastructure.gateway.dto.SkuStockRequestDTO;
import com.yue.order.infrastructure.gateway.response.GatewayResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Mall 服务 Feign 客户端（对应 mall 服务的库存接口）
 */
@FeignClient(name = "mall")
public interface IMallService {

    /**
     * 锁定库存（下单时调用）
     */
    @PostMapping("/api/v1/sku/lock_stock")
    GatewayResponse<Boolean> lockStock(@RequestBody SkuStockRequestDTO request);

    /**
     * 释放库存锁定（退单/取消时调用）
     */
    @PostMapping("/api/v1/sku/unlock_stock")
    GatewayResponse<Boolean> unlockStock(@RequestBody SkuStockRequestDTO request);
}
