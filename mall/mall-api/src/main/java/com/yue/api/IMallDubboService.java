package com.yue.api;

import com.yue.api.dto.SkuStockRequestDTO;

/**
 * Mall 服务 Dubbo RPC 接口（Triple 协议）
 * 供 order-service 调用锁库/解库
 */
public interface IMallDubboService {

    Boolean lockStock(SkuStockRequestDTO request);

    Boolean unlockStock(SkuStockRequestDTO request);
}
