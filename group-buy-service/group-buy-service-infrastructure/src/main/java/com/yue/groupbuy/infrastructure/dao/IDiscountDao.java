package com.yue.groupbuy.infrastructure.dao;

import com.yue.groupbuy.infrastructure.dao.po.Discount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IDiscountDao {

    Discount queryByDiscountId(@Param("discountId") String discountId);
}
