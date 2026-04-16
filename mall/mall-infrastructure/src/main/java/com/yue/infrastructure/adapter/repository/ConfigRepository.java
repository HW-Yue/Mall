package com.yue.infrastructure.adapter.repository;

import com.yue.infrastructure.dao.*;
import com.yue.infrastructure.dao.po.*;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 后台配置仓储：封装 mall_db 配置表 CRUD
 * 仅包含 activity_type / category / sku / sku_resource_detail
 */
@Repository
public class ConfigRepository {

    @Resource
    private ISkuDao skuDao;
    @Resource
    private ISkuResourceDetailDao skuResourceDetailDao;
    @Resource
    private IActivityTypeDao activityTypeDao;
    @Resource
    private ICategoryDao categoryDao;

    // ---------- 活动类型 ----------
    public List<ActivityType> listActivityTypes() {
        return activityTypeDao.queryActivityTypeListAll();
    }

    public ActivityType getActivityType(Integer id) {
        return activityTypeDao.queryActivityTypeById(id);
    }

    public ActivityType getActivityTypeByCode(String typeCode) {
        return activityTypeDao.queryActivityTypeByCode(typeCode);
    }

    public int insertActivityType(ActivityType po) {
        return activityTypeDao.insert(po);
    }

    public int updateActivityType(ActivityType po) {
        return activityTypeDao.update(po);
    }

    public int deleteActivityType(Integer id) {
        return activityTypeDao.deleteById(id);
    }

    // ---------- 商品类目 ----------
    public List<Category> listCategories() {
        return categoryDao.queryCategoryListAll();
    }

    public Category getCategory(Integer id) {
        return categoryDao.queryCategoryById(id);
    }

    public int insertCategory(Category po) {
        return categoryDao.insert(po);
    }

    public int updateCategory(Category po) {
        return categoryDao.update(po);
    }

    public int deleteCategory(Integer id) {
        return categoryDao.deleteById(id);
    }

    // ---------- 商品 SKU ----------
    public List<Sku> listSkus() {
        return skuDao.querySkuList();
    }

    public Sku getSku(String goodsId) {
        return skuDao.querySkuByGoodsId(goodsId);
    }

    public int insertSku(Sku po) {
        return skuDao.insert(po);
    }

    public int updateSku(Sku po) {
        return skuDao.update(po);
    }

    public int deleteSku(String goodsId) {
        return skuDao.deleteByGoodsId(goodsId);
    }

    // ---------- 商品资源详情 ----------
    public List<SkuResourceDetail> listSkuResourceDetailsByGoodsId(String goodsId) {
        return skuResourceDetailDao.listByGoodsId(goodsId);
    }

    public SkuResourceDetail getSkuResourceDetail(Integer id) {
        return skuResourceDetailDao.queryById(id);
    }

    public SkuResourceDetail getSkuResourceDetailByGoodsIdAndResKey(String goodsId, String resKey) {
        return skuResourceDetailDao.getByGoodsIdAndResKey(goodsId, resKey);
    }

    public List<SkuResourceDetail> listSkuResourceDetails(SkuResourceDetail condition) {
        return skuResourceDetailDao.queryList(condition);
    }

    public int insertSkuResourceDetail(SkuResourceDetail po) {
        return skuResourceDetailDao.insert(po);
    }

    public int updateSkuResourceDetail(SkuResourceDetail po) {
        return skuResourceDetailDao.update(po);
    }

    public int deleteSkuResourceDetailById(Integer id) {
        return skuResourceDetailDao.deleteById(id);
    }

    public int deleteSkuResourceDetailByGoodsId(String goodsId) {
        return skuResourceDetailDao.deleteByGoodsId(goodsId);
    }
}
