package com.yue.seckill.infrastructure.dao;

import com.yue.seckill.infrastructure.dao.po.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品类目 DAO
 */
@Mapper
public interface ICategoryDao {

    Category queryById(Integer id);

}
