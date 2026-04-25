package com.yue.trigger.http;

import com.yue.api.dto.SkuStockRequestDTO;
import com.yue.api.response.Response;
import com.yue.trigger.service.sku.SkuStockAppService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * SKU 库存管理接口，供 order-service 通过 Feign 调用
 */
@RestController
@RequestMapping("/api/v1/sku/")
public class SkuController {

    @Resource
    private SkuStockAppService skuStockAppService;

    /**
     * 锁定库存（下单时调用）
     * 悲观锁保证可售库存一致性
     */
    @PostMapping("lock_stock")
    public Response<Boolean> lockStock(@RequestBody SkuStockRequestDTO request) {
        return skuStockAppService.lockStock(request);
    }

    /**
     * 释放库存锁定（退单/超时取消时调用）
     */
    @PostMapping("unlock_stock")
    public Response<Boolean> unlockStock(@RequestBody SkuStockRequestDTO request) {
        return skuStockAppService.unlockStock(request);
    }
}
