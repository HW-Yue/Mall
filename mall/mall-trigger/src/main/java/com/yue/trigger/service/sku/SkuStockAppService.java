package com.yue.trigger.service.sku;

import com.yue.api.dto.SkuStockRequestDTO;
import com.yue.api.response.Response;
import com.yue.infrastructure.dao.ISkuDao;
import com.yue.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

/**
 * SKU 锁库/解锁（与 SkuController 共用逻辑）
 */
@Slf4j
@Service
public class SkuStockAppService {

    @Resource
    private ISkuDao skuDao;

    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> lockStock(SkuStockRequestDTO request) {
        try {
            if (request == null || request.getGoodsId() == null || request.getCount() == null || request.getCount() <= 0) {
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info(ResponseCode.ILLEGAL_PARAMETER.getInfo())
                        .data(false)
                        .build();
            }
            com.yue.infrastructure.dao.po.Sku sku = skuDao.selectByGoodsIdForUpdate(request.getGoodsId());
            if (sku == null) {
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("商品不存在: " + request.getGoodsId())
                        .data(false)
                        .build();
            }
            int available = sku.getTotalStock() - sku.getLockedStock();
            if (available < request.getCount()) {
                return Response.<Boolean>builder()
                        .code(ResponseCode.UN_ERROR.getCode())
                        .info("库存不足")
                        .data(false)
                        .build();
            }
            int rows = skuDao.addLockedStock(request.getGoodsId(), request.getCount());
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(rows > 0)
                    .build();
        } catch (Exception e) {
            log.error("lockStock error goodsId:{} count:{}", request == null ? null : request.getGoodsId(), request == null ? null : request.getCount(), e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .data(false)
                    .build();
        }
    }

    public Response<Boolean> unlockStock(SkuStockRequestDTO request) {
        try {
            if (request == null || request.getGoodsId() == null || request.getCount() == null || request.getCount() <= 0) {
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info(ResponseCode.ILLEGAL_PARAMETER.getInfo())
                        .data(false)
                        .build();
            }
            int rows = skuDao.subLockedStock(request.getGoodsId(), request.getCount());
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(rows > 0)
                    .build();
        } catch (Exception e) {
            log.error("unlockStock error goodsId:{} count:{}", request == null ? null : request.getGoodsId(), request == null ? null : request.getCount(), e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .data(false)
                    .build();
        }
    }
}
