package com.yue.groupbuy.infrastructure.dao;

import com.yue.groupbuy.infrastructure.dao.po.Sku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ISkuDao {

    Sku queryByGoodsId(@Param("goodsId") String goodsId);

    List<Sku> queryAllSkuList();
}
