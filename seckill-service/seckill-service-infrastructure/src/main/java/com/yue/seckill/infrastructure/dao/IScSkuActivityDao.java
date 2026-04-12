package com.yue.seckill.infrastructure.dao;

import com.yue.seckill.infrastructure.dao.po.ScSkuActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 渠道-商品-活动映射 DAO
 */
@Mapper
public interface IScSkuActivityDao {

    ScSkuActivity queryByScGoodsId(@Param("source") String source,
                                    @Param("channel") String channel,
                                    @Param("goodsId") String goodsId);

    List<ScSkuActivity> queryByActivityId(Long activityId);

}
