package com.yue.groupbuy.infrastructure.dao;

import com.yue.groupbuy.infrastructure.dao.po.SkuResourceDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ISkuResourceDetailDao {

    List<SkuResourceDetail> listByGoodsId(@Param("goodsId") String goodsId);
}
