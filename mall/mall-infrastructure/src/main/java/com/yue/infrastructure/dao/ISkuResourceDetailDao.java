package com.yue.infrastructure.dao;

import com.yue.infrastructure.dao.po.SkuResourceDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品资源详情表 Dao
 */
@Mapper
public interface ISkuResourceDetailDao {

    List<SkuResourceDetail> listByGoodsId(@Param("goodsId") String goodsId);

    SkuResourceDetail getByGoodsIdAndResKey(@Param("goodsId") String goodsId, @Param("resKey") String resKey);

    SkuResourceDetail queryById(@Param("id") Integer id);

    List<SkuResourceDetail> queryList(SkuResourceDetail condition);

    int insert(SkuResourceDetail po);

    int update(SkuResourceDetail po);

    int deleteById(@Param("id") Integer id);

    int deleteByGoodsId(@Param("goodsId") String goodsId);
}
