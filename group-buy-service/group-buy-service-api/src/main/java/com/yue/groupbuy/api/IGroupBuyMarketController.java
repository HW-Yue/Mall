package com.yue.groupbuy.api;

import com.yue.groupbuy.api.dto.*;
import com.yue.groupbuy.api.response.Response;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@RequestMapping("/api/v1/group-buy/market/")
public interface IGroupBuyMarketController {

    /**
     * 查询拼团商品列表
     */
    @GetMapping("query_goods_list")
    Response<QueryGroupBuyGoodsListResponseDTO> queryGroupBuyGoodsList();

    /**
     * 查询拼团市场配置（聚合接口：试算+参团记录+统计）
     */
    @PostMapping("query_group_buy_market_config")
    Response<GoodsMarketResponseDTO> queryGroupBuyMarketConfig(@RequestBody GoodsMarketRequestDTO request);

}
