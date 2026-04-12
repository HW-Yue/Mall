package com.yue.infrastructure.dao;

import com.yue.infrastructure.dao.po.Category;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 商品类目 Dao
 */
@Mapper
public interface ICategoryDao {

    Category queryCategoryById(Integer id);

    Category queryCategoryByCode(String code);

    List<Category> queryCategoryList();

    /** 查询全部（后台配置用，不筛 status） */
    List<Category> queryCategoryListAll();

    int insert(Category po);

    int update(Category po);

    int deleteById(Integer id);
}
