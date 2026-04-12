/**
 * @description: 商品首页查询
 * @author: 29874
 * @date: 2026/3/11 17:05
 */

package com.yue.trigger.http;

import com.yue.api.IIndexController;
import com.yue.api.dto.*;
import com.yue.api.response.Response;
import com.yue.trigger.service.mall.IndexAppServiceImpl;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController()
@RequestMapping("/api/v1/mall/index/")
public class IndexController implements IIndexController {

    @Resource
    private IndexAppServiceImpl indexAppService;

    @RequestMapping(value = "query_category_type_list", method = RequestMethod.GET)
    @Override
    public Response<List<CategoryTypeDTO>> queryCategoryTypeList() {
        return indexAppService.queryCategoryTypeList();
    }

    @RequestMapping(value = "query_goods_page", method = RequestMethod.POST)
    @Override
    public Response<GoodsPageResponseDTO> queryGoodsPage(@RequestBody GoodsPageRequestDTO request) {
        return indexAppService.queryGoodsPage(request);
    }

    @RequestMapping(value = "query_sku_detail", method = RequestMethod.POST)
    @Override
    public Response<GoodsDetailResponseDTO> querySkuDetail(@RequestBody GoodsDetailRequestDTO requestDTO) {
        return indexAppService.querySkuDetail(requestDTO);
    }

    @RequestMapping(value = "query_activity_goods", method = RequestMethod.GET)
    @Override
    public Response<ActivityGoodsDTO> queryActivityGoods() {
        return indexAppService.queryActivityGoods();
    }
}
