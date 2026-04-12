package com.yue.api;

import com.yue.api.dto.ActivityTypeDTO;
import com.yue.api.dto.CategoryDTO;
import com.yue.api.dto.SkuDTO;
import com.yue.api.response.Response;

import java.util.List;

/**
 * 后端配置 HTTP/Feign 契约（mall_db 管辖：活动类型、商品类目、商品 SKU）
 * 拼团/秒杀/折扣/渠道映射/人群标签已迁移至各自微服务，不再通过此接口提供
 */
public interface IBackendConfigController {

    Response<List<ActivityTypeDTO>> listActivityTypes();

    Response<ActivityTypeDTO> getActivityType(Integer id);

    Response<Boolean> saveActivityType(ActivityTypeDTO dto);

    Response<Boolean> updateActivityType(ActivityTypeDTO dto);

    Response<Boolean> deleteActivityType(Integer id);

    Response<List<CategoryDTO>> listCategories();

    Response<CategoryDTO> getCategory(Integer id);

    Response<Boolean> saveCategory(CategoryDTO dto);

    Response<Boolean> updateCategory(CategoryDTO dto);

    Response<Boolean> deleteCategory(Integer id);

    Response<List<SkuDTO>> listSkus();

    Response<SkuDTO> getSku(String goodsId);

    Response<Boolean> saveSku(SkuDTO dto);

    Response<Boolean> deleteSku(String goodsId);

    Response<Boolean> updateDccConfig(String key, String value);
}
