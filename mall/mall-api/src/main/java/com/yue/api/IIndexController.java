package com.yue.api;

import com.yue.api.dto.ActivityGoodsDTO;
import com.yue.api.dto.CategoryTypeDTO;
import com.yue.api.dto.GoodsDetailRequestDTO;
import com.yue.api.dto.GoodsDetailResponseDTO;
import com.yue.api.dto.GoodsPageRequestDTO;
import com.yue.api.dto.GoodsPageResponseDTO;
import com.yue.api.response.Response;

import java.util.List;

/**
 * 商品首页 HTTP 契约
 */
public interface IIndexController {

    Response<List<CategoryTypeDTO>> queryCategoryTypeList();

    Response<GoodsPageResponseDTO> queryGoodsPage(GoodsPageRequestDTO request);

    Response<GoodsDetailResponseDTO> querySkuDetail(GoodsDetailRequestDTO requestDTO);

    Response<ActivityGoodsDTO> queryActivityGoods();
}
