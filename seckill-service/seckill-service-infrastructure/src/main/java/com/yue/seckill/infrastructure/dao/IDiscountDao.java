package com.yue.seckill.infrastructure.dao;

import com.yue.seckill.infrastructure.dao.po.Discount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 折扣配置 DAO
 */
@Mapper
public interface IDiscountDao {

    Discount queryByDiscountId(String discountId);

}
