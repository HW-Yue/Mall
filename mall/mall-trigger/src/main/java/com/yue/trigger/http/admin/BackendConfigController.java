package com.yue.trigger.http.admin;

import com.yue.api.IBackendConfigController;
import com.yue.api.dto.ActivityTypeDTO;
import com.yue.api.dto.CategoryDTO;
import com.yue.api.dto.SkuDTO;
import com.yue.api.response.Response;
import com.yue.trigger.service.admin.BackendConfigAppServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 后端配置 HTTP 入口，业务在 {@link com.yue.trigger.service.admin.BackendConfigAppServiceImpl}
 */
@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/gbm/config/")
public class BackendConfigController implements IBackendConfigController {

    @Resource
    private BackendConfigAppServiceImpl backendConfigAppService;

    @Override
    @RequestMapping(value = "activity_types", method = RequestMethod.GET)
    public Response<List<ActivityTypeDTO>> listActivityTypes() {
        return backendConfigAppService.listActivityTypes();
    }

    @Override
    @RequestMapping(value = "activity_type/{id}", method = RequestMethod.GET)
    public Response<ActivityTypeDTO> getActivityType(@PathVariable Integer id) {
        return backendConfigAppService.getActivityType(id);
    }

    @Override
    public Response<Boolean> updateDccConfig(String key, String value) {
        return null;
    }

    @Override
    @RequestMapping(value = "activity_type", method = RequestMethod.POST)
    public Response<Boolean> saveActivityType(@RequestBody ActivityTypeDTO dto) {
        return backendConfigAppService.saveActivityType(dto);
    }

    @Override
    @RequestMapping(value = "activity_type", method = RequestMethod.PUT)
    public Response<Boolean> updateActivityType(@RequestBody ActivityTypeDTO dto) {
        return backendConfigAppService.updateActivityType(dto);
    }

    @Override
    @RequestMapping(value = "activity_type/{id}", method = RequestMethod.DELETE)
    public Response<Boolean> deleteActivityType(@PathVariable Integer id) {
        return backendConfigAppService.deleteActivityType(id);
    }

    @Override
    @RequestMapping(value = "categories", method = RequestMethod.GET)
    public Response<List<CategoryDTO>> listCategories() {
        return backendConfigAppService.listCategories();
    }

    @Override
    @RequestMapping(value = "category/{id}", method = RequestMethod.GET)
    public Response<CategoryDTO> getCategory(@PathVariable Integer id) {
        return backendConfigAppService.getCategory(id);
    }

    @Override
    @RequestMapping(value = "category", method = RequestMethod.POST)
    public Response<Boolean> saveCategory(@RequestBody CategoryDTO dto) {
        return backendConfigAppService.saveCategory(dto);
    }

    @Override
    @RequestMapping(value = "category", method = RequestMethod.PUT)
    public Response<Boolean> updateCategory(@RequestBody CategoryDTO dto) {
        return backendConfigAppService.updateCategory(dto);
    }

    @Override
    @RequestMapping(value = "category/{id}", method = RequestMethod.DELETE)
    public Response<Boolean> deleteCategory(@PathVariable Integer id) {
        return backendConfigAppService.deleteCategory(id);
    }

    @Override
    @RequestMapping(value = "skus", method = RequestMethod.GET)
    public Response<List<SkuDTO>> listSkus() {
        return backendConfigAppService.listSkus();
    }

    @Override
    @RequestMapping(value = "sku/{goodsId}", method = RequestMethod.GET)
    public Response<SkuDTO> getSku(@PathVariable String goodsId) {
        return backendConfigAppService.getSku(goodsId);
    }

    @Override
    @RequestMapping(value = "sku", method = RequestMethod.POST)
    public Response<Boolean> saveSku(@RequestBody SkuDTO dto) {
        return backendConfigAppService.saveSku(dto);
    }

    @Override
    @RequestMapping(value = "sku/{goodsId}", method = RequestMethod.DELETE)
    public Response<Boolean> deleteSku(@PathVariable String goodsId) {
        return backendConfigAppService.deleteSku(goodsId);
    }

}
