package com.yue.seckill.infrastructure.dao;

import com.yue.seckill.infrastructure.dao.po.Sku;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 商品信息 DAO
 */
@Mapper
public interface ISkuDao {

    Sku queryByGoodsId(String goodsId);

    List<Sku> queryAll();

}
