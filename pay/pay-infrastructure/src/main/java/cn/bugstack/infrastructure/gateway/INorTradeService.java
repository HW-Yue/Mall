package cn.bugstack.infrastructure.gateway;

import cn.bugstack.infrastructure.gateway.dto.*;
import cn.bugstack.infrastructure.gateway.response.Response;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * 电商服务 - 普通商品（无营销）交易接口
 * 使用 Retrofit2 调用：锁单、结算
 */
public interface INorTradeService {

    /**
     * 普通商品锁单
     * POST /api/v1/nor/trade/lock_pay_order
     */
    @POST("api/v1/nor/trade/lock_pay_order")
    Call<Response<LockPayOrderResponseDTO>> lockPayOrder(@Body LockPayOrderRequestDTO requestDTO);

    /**
     * 普通商品结算（直接结算，无责任链过滤）
     * POST /api/v1/nor/trade/settlement_market_pay_order
     */
    @POST("api/v1/nor/trade/settlement_market_pay_order")
    Call<Response<SettlementMarketPayOrderResponseDTO>> settlementMarketPayOrder(@Body SettlementMarketPayOrderRequestDTO requestDTO);
}
