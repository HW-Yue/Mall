package com.yue.trigger.rpc;

import com.yue.api.IMallDubboService;
import com.yue.api.dto.SkuStockRequestDTO;
import com.yue.trigger.service.sku.SkuStockAppService;
import com.yue.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import jakarta.annotation.Resource;

@Slf4j
@DubboService
public class MallDubboServiceImpl implements IMallDubboService {

    @Resource
    private SkuStockAppService skuStockAppService;

    @Override
    public Boolean lockStock(SkuStockRequestDTO request) {
        var resp = skuStockAppService.lockStock(request);
        if (resp == null || !ResponseCode.SUCCESS.getCode().equals(resp.getCode())) {
            String info = resp != null ? resp.getInfo() : "lockStock failed";
            log.warn("dubbo lockStock 失败 goodsId:{} reason:{}", request.getGoodsId(), info);
            throw new RuntimeException("库存锁定失败: " + info);
        }
        return Boolean.TRUE.equals(resp.getData());
    }

    @Override
    public Boolean unlockStock(SkuStockRequestDTO request) {
        var resp = skuStockAppService.unlockStock(request);
        if (resp == null || !ResponseCode.SUCCESS.getCode().equals(resp.getCode())) {
            String info = resp != null ? resp.getInfo() : "unlockStock failed";
            log.warn("dubbo unlockStock 失败 goodsId:{} reason:{}", request.getGoodsId(), info);
            return false;
        }
        return Boolean.TRUE.equals(resp.getData());
    }
}
