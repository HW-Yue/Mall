package com.yue.trigger.http;

import com.yue.api.dto.SkuStockRequestDTO;
import com.yue.api.response.Response;
import com.yue.infrastructure.dao.ISkuDao;
import com.yue.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * SKU 库存管理接口，供 order-service 通过 Feign 调用
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sku/")
public class SkuController {

    @Resource
    private ISkuDao skuDao;

    /**
     * 锁定库存（下单时调用）
     * 悲观锁保证可售库存一致性
     */
    @PostMapping("lock_stock")
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> lockStock(@RequestBody SkuStockRequestDTO request) {
        try {
            if (request == null || request.getGoodsId() == null || request.getCount() == null || request.getCount() <= 0) {
                return Response.<Boolean>builder().code(ResponseCode.ILLEGAL_PARAMETER.getCode()).info(ResponseCode.ILLEGAL_PARAMETER.getInfo()).data(false).build();
            }
            // FOR UPDATE 保证并发安全
            com.yue.infrastructure.dao.po.Sku sku = skuDao.selectByGoodsIdForUpdate(request.getGoodsId());
            if (sku == null) {
                return Response.<Boolean>builder().code(ResponseCode.ILLEGAL_PARAMETER.getCode()).info("商品不存在: " + request.getGoodsId()).data(false).build();
            }
            int available = sku.getTotalStock() - sku.getLockedStock();
            if (available < request.getCount()) {
                return Response.<Boolean>builder().code(ResponseCode.UN_ERROR.getCode()).info("库存不足").data(false).build();
            }
            int rows = skuDao.addLockedStock(request.getGoodsId(), request.getCount());
            return Response.<Boolean>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(rows > 0).build();
        } catch (Exception e) {
            log.error("lockStock error goodsId:{} count:{}", request == null ? null : request.getGoodsId(), request == null ? null : request.getCount(), e);
            return Response.<Boolean>builder().code(ResponseCode.UN_ERROR.getCode()).info(e.getMessage()).data(false).build();
        }
    }

    /**
     * 释放库存锁定（退单/超时取消时调用）
     */
    @PostMapping("unlock_stock")
    public Response<Boolean> unlockStock(@RequestBody SkuStockRequestDTO request) {
        try {
            if (request == null || request.getGoodsId() == null || request.getCount() == null || request.getCount() <= 0) {
                return Response.<Boolean>builder().code(ResponseCode.ILLEGAL_PARAMETER.getCode()).info(ResponseCode.ILLEGAL_PARAMETER.getInfo()).data(false).build();
            }
            int rows = skuDao.subLockedStock(request.getGoodsId(), request.getCount());
            return Response.<Boolean>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(rows > 0).build();
        } catch (Exception e) {
            log.error("unlockStock error goodsId:{} count:{}", request == null ? null : request.getGoodsId(), request == null ? null : request.getCount(), e);
            return Response.<Boolean>builder().code(ResponseCode.UN_ERROR.getCode()).info(e.getMessage()).data(false).build();
        }
    }
}
