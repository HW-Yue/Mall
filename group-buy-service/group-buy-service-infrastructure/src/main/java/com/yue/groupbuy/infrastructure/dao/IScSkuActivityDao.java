package com.yue.groupbuy.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IScSkuActivityDao {

    Long queryActivityIdBySCGoodsId(@Param("source") String source,
                                    @Param("channel") String channel,
                                    @Param("goodsId") String goodsId);

}
