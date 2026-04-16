package com.yue.trigger.service.admin;

import com.yue.api.dto.ActivityTypeDTO;
import com.yue.api.dto.CategoryDTO;
import com.yue.api.dto.SkuDTO;
import com.yue.api.response.Response;
import com.yue.infrastructure.adapter.repository.ConfigRepository;
import com.yue.infrastructure.dao.po.ActivityType;
import com.yue.infrastructure.dao.po.Category;
import com.yue.infrastructure.dao.po.Sku;
import com.yue.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 后端配置接口：活动类型、商品类目、商品 SKU（mall_db 管辖范围）
 * 拼团/秒杀/折扣/渠道映射/人群标签已迁移至各自微服务 DB，此处不再提供
 */
@Slf4j
@Service
public class BackendConfigAppServiceImpl {

    @Resource
    private ConfigRepository configRepository;
    @Resource
    private DCCAppServiceImpl dccAppService;

    public Response<List<ActivityTypeDTO>> listActivityTypes() {
        try {
            log.info("后台配置 查询活动类型列表");
            List<ActivityType> list = configRepository.listActivityTypes();
            List<ActivityTypeDTO> dtoList = list == null ? new ArrayList<>() : list.stream().map(this::toActivityTypeDTO).collect(Collectors.toList());
            log.info("后台配置 查询活动类型列表完成 size:{}", dtoList.size());
            return Response.<List<ActivityTypeDTO>>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(dtoList).build();
        } catch (Exception e) {
            log.warn("listActivityTypes fail", e);
            return Response.<List<ActivityTypeDTO>>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(new ArrayList<>()).build();
        }
    }

    public Response<ActivityTypeDTO> getActivityType(Integer id) {
        try {
            log.info("后台配置 查询活动类型 id:{}", id);
            ActivityType po = configRepository.getActivityType(id);
            if (po == null) {
                log.info("后台配置 活动类型不存在 id:{}", id);
                return Response.<ActivityTypeDTO>builder().code(ResponseCode.ILLEGAL_PARAMETER.getCode()).info("活动类型不存在").build();
            }
            log.info("后台配置 查询活动类型成功 id:{} code:{}", id, po.getTypeCode());
            return Response.<ActivityTypeDTO>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(toActivityTypeDTO(po)).build();
        } catch (Exception e) {
            log.warn("getActivityType fail", e);
            return Response.<ActivityTypeDTO>builder().code(ResponseCode.UN_ERROR.getCode()).info(e.getMessage()).build();
        }
    }

    public Response<Boolean> saveActivityType(ActivityTypeDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getTypeName()) || StringUtils.isBlank(dto.getTypeCode())) {
            log.info("后台配置 新增活动类型 参数非法");
            return Response.<Boolean>builder().code(ResponseCode.ILLEGAL_PARAMETER.getCode()).info(ResponseCode.ILLEGAL_PARAMETER.getInfo()).build();
        }
        try {
            log.info("后台配置 新增活动类型 typeCode:{} typeName:{}", dto.getTypeCode(), dto.getTypeName());
            ActivityType po = toActivityTypePO(dto);
            int n = configRepository.insertActivityType(po);
            log.info("后台配置 新增活动类型完成 typeCode:{} 影响行:{}", dto.getTypeCode(), n);
            return Response.<Boolean>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(n > 0).build();
        } catch (Exception e) {
            log.warn("saveActivityType fail", e);
            return Response.<Boolean>builder().code(ResponseCode.UN_ERROR.getCode()).info(e.getMessage()).build();
        }
    }

    public Response<Boolean> updateActivityType(ActivityTypeDTO dto) {
        if (dto == null || dto.getId() == null) {
            log.info("后台配置 更新活动类型 参数非法");
            return Response.<Boolean>builder().code(ResponseCode.ILLEGAL_PARAMETER.getCode()).info(ResponseCode.ILLEGAL_PARAMETER.getInfo()).build();
        }
        try {
            log.info("后台配置 更新活动类型 id:{} typeCode:{}", dto.getId(), dto.getTypeCode());
            ActivityType po = toActivityTypePO(dto);
            po.setId(dto.getId());
            int n = configRepository.updateActivityType(po);
            log.info("后台配置 更新活动类型完成 id:{} 影响行:{}", dto.getId(), n);
            return Response.<Boolean>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(n > 0).build();
        } catch (Exception e) {
            log.warn("updateActivityType fail", e);
            return Response.<Boolean>builder().code(ResponseCode.UN_ERROR.getCode()).info(e.getMessage()).build();
        }
    }

    public Response<Boolean> deleteActivityType(Integer id) {
        try {
            log.info("后台配置 删除活动类型 id:{}", id);
            int n = configRepository.deleteActivityType(id);
            log.info("后台配置 删除活动类型完成 id:{} 影响行:{}", id, n);
            return Response.<Boolean>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(n > 0).build();
        } catch (Exception e) {
            log.warn("deleteActivityType fail", e);
            return Response.<Boolean>builder().code(ResponseCode.UN_ERROR.getCode()).info(e.getMessage()).build();
        }
    }

    public Response<List<CategoryDTO>> listCategories() {
        try {
            log.info("后台配置 查询商品类目列表");
            List<Category> list = configRepository.listCategories();
            List<CategoryDTO> dtoList = list == null ? new ArrayList<>() : list.stream().map(this::toCategoryDTO).collect(Collectors.toList());
            log.info("后台配置 查询商品类目列表完成 size:{}", dtoList.size());
            return Response.<List<CategoryDTO>>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(dtoList).build();
        } catch (Exception e) {
            log.warn("listCategories fail", e);
            return Response.<List<CategoryDTO>>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(new ArrayList<>()).build();
        }
    }

    public Response<CategoryDTO> getCategory(Integer id) {
        try {
            log.info("后台配置 查询商品类目 id:{}", id);
            Category po = configRepository.getCategory(id);
            if (po == null) {
                log.info("后台配置 类目不存在 id:{}", id);
                return Response.<CategoryDTO>builder().code(ResponseCode.ILLEGAL_PARAMETER.getCode()).info("类目不存在").build();
            }
            log.info("后台配置 查询商品类目成功 id:{} code:{}", id, po.getCode());
            return Response.<CategoryDTO>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(toCategoryDTO(po)).build();
        } catch (Exception e) {
            log.warn("getCategory fail", e);
            return Response.<CategoryDTO>builder().code(ResponseCode.UN_ERROR.getCode()).info(e.getMessage()).build();
        }
    }

    public Response<Boolean> saveCategory(CategoryDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getName()) || StringUtils.isBlank(dto.getCode())) {
            log.info("后台配置 新增类目 参数非法");
            return Response.<Boolean>builder().code(ResponseCode.ILLEGAL_PARAMETER.getCode()).info(ResponseCode.ILLEGAL_PARAMETER.getInfo()).build();
        }
        try {
            log.info("后台配置 新增类目 code:{} name:{}", dto.getCode(), dto.getName());
            Category po = toCategoryPO(dto);
            int n = configRepository.insertCategory(po);
            log.info("后台配置 新增类目完成 code:{} 影响行:{}", dto.getCode(), n);
            return Response.<Boolean>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(n > 0).build();
        } catch (Exception e) {
            log.warn("saveCategory fail", e);
            return Response.<Boolean>builder().code(ResponseCode.UN_ERROR.getCode()).info(e.getMessage()).build();
        }
    }

    public Response<Boolean> updateCategory(CategoryDTO dto) {
        if (dto == null || dto.getId() == null) {
            log.info("后台配置 更新类目 参数非法");
            return Response.<Boolean>builder().code(ResponseCode.ILLEGAL_PARAMETER.getCode()).info(ResponseCode.ILLEGAL_PARAMETER.getInfo()).build();
        }
        try {
            log.info("后台配置 更新类目 id:{} code:{}", dto.getId(), dto.getCode());
            Category po = toCategoryPO(dto);
            po.setId(dto.getId());
            int n = configRepository.updateCategory(po);
            log.info("后台配置 更新类目完成 id:{} 影响行:{}", dto.getId(), n);
            return Response.<Boolean>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(n > 0).build();
        } catch (Exception e) {
            log.warn("updateCategory fail", e);
            return Response.<Boolean>builder().code(ResponseCode.UN_ERROR.getCode()).info(e.getMessage()).build();
        }
    }

    public Response<Boolean> deleteCategory(Integer id) {
        try {
            log.info("后台配置 删除类目 id:{}", id);
            int n = configRepository.deleteCategory(id);
            log.info("后台配置 删除类目完成 id:{} 影响行:{}", id, n);
            return Response.<Boolean>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(n > 0).build();
        } catch (Exception e) {
            log.warn("deleteCategory fail", e);
            return Response.<Boolean>builder().code(ResponseCode.UN_ERROR.getCode()).info(e.getMessage()).build();
        }
    }

    public Response<List<SkuDTO>> listSkus() {
        log.info("后台配置 查询 SKU 列表");
        List<Sku> list = configRepository.listSkus();
        List<SkuDTO> dtoList = list == null ? new ArrayList<>() : list.stream().map(this::toSkuDTO).collect(Collectors.toList());
        log.info("后台配置 查询 SKU 列表完成 size:{}", dtoList.size());
        return Response.<List<SkuDTO>>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(dtoList).build();
    }

    public Response<SkuDTO> getSku(String goodsId) {
        log.info("后台配置 查询 SKU goodsId:{}", goodsId);
        Sku po = configRepository.getSku(goodsId);
        if (po == null) {
            log.info("后台配置 SKU 不存在 goodsId:{}", goodsId);
            return Response.<SkuDTO>builder().code(ResponseCode.ILLEGAL_PARAMETER.getCode()).info("商品不存在").build();
        }
        log.info("后台配置 查询 SKU 成功 goodsId:{}", goodsId);
        return Response.<SkuDTO>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(toSkuDTO(po)).build();
    }

    public Response<Boolean> saveSku(SkuDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getGoodsId()) || StringUtils.isBlank(dto.getGoodsName()) || dto.getOriginalPrice() == null) {
            log.info("后台配置 保存 SKU 参数非法");
            return Response.<Boolean>builder().code(ResponseCode.ILLEGAL_PARAMETER.getCode()).info(ResponseCode.ILLEGAL_PARAMETER.getInfo()).build();
        }
        Sku po = toSkuPO(dto);
        if (configRepository.getSku(dto.getGoodsId()) != null) {
            log.info("后台配置 更新 SKU goodsId:{}", dto.getGoodsId());
            int n = configRepository.updateSku(po);
            log.info("后台配置 更新 SKU 完成 goodsId:{} 影响行:{}", dto.getGoodsId(), n);
            return Response.<Boolean>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(n > 0).build();
        }
        log.info("后台配置 新增 SKU goodsId:{}", dto.getGoodsId());
        int n = configRepository.insertSku(po);
        log.info("后台配置 新增 SKU 完成 goodsId:{} 影响行:{}", dto.getGoodsId(), n);
        return Response.<Boolean>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(n > 0).build();
    }

    public Response<Boolean> deleteSku(String goodsId) {
        log.info("后台配置 删除 SKU goodsId:{}", goodsId);
        int n = configRepository.deleteSku(goodsId);
        log.info("后台配置 删除 SKU 完成 goodsId:{} 影响行:{}", goodsId, n);
        return Response.<Boolean>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(n > 0).build();
    }

    public Response<Boolean> updateDccConfig(String key, String value) {
        return dccAppService.updateConfig(key, value);
    }

    // ---------- PO <-> DTO 转换 ----------
    private ActivityTypeDTO toActivityTypeDTO(ActivityType po) {
        ActivityTypeDTO dto = new ActivityTypeDTO();
        dto.setId(po.getId());
        dto.setTypeName(po.getTypeName());
        dto.setTypeCode(po.getTypeCode());
        dto.setStatus(po.getStatus());
        return dto;
    }

    private ActivityType toActivityTypePO(ActivityTypeDTO dto) {
        ActivityType po = new ActivityType();
        po.setTypeName(dto.getTypeName());
        po.setTypeCode(dto.getTypeCode());
        po.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        return po;
    }

    private CategoryDTO toCategoryDTO(Category po) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(po.getId());
        dto.setName(po.getName());
        dto.setCode(po.getCode());
        dto.setIconUrl(po.getIconUrl());
        dto.setSortOrder(po.getSortOrder());
        dto.setStatus(po.getStatus());
        return dto;
    }

    private Category toCategoryPO(CategoryDTO dto) {
        Category po = new Category();
        po.setName(dto.getName());
        po.setCode(dto.getCode());
        po.setIconUrl(dto.getIconUrl());
        po.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        po.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        return po;
    }

    private SkuDTO toSkuDTO(Sku po) {
        SkuDTO dto = new SkuDTO();
        dto.setId(po.getId());
        dto.setGoodsId(po.getGoodsId());
        dto.setGoodsName(po.getGoodsName());
        dto.setGoodsImageUrl(po.getGoodsImageUrl());
        dto.setOriginalPrice(po.getOriginalPrice());
        dto.setCategoryId(po.getCategoryId());
        dto.setTotalStock(po.getTotalStock());
        dto.setLockedStock(po.getLockedStock());
        return dto;
    }

    private Sku toSkuPO(SkuDTO dto) {
        return Sku.builder()
                .goodsId(dto.getGoodsId())
                .goodsName(dto.getGoodsName())
                .goodsImageUrl(dto.getGoodsImageUrl())
                .originalPrice(dto.getOriginalPrice())
                .categoryId(dto.getCategoryId())
                .totalStock(dto.getTotalStock() != null ? dto.getTotalStock() : 0)
                .lockedStock(dto.getLockedStock() != null ? dto.getLockedStock() : 0)
                .build();
    }
}
