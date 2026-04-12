/**
 * @description:
 * @author: 29874
 * @date: 2026/3/11 21:01
 */

package cn.bugstack.infrastructure.gateway;

import cn.bugstack.infrastructure.gateway.dto.GoodsDetailRequestDTO;
import cn.bugstack.infrastructure.gateway.dto.GoodsDetailResponseDTO;
import cn.bugstack.infrastructure.gateway.response.Response;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface IProductService {
    /**
     * 查询商品详细信息
     *
     * @param requestDTO 锁单商品信息
     * @return 锁单结果信息
     */
    @POST("api/v1/index/query_sku_detail")
    Call<Response<GoodsDetailResponseDTO>> querySkuDetail(@Body GoodsDetailRequestDTO requestDTO);

}
