package com.yue.seckill.infrastructure.dao;

import com.yue.seckill.infrastructure.dao.po.SeckillActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 秒杀活动 DAO
 */
@Mapper
public interface ISeckillActivityDao {

    SeckillActivity queryByActivityId(Long activityId);

    List<SeckillActivity> queryEffectiveActivities();

    int deductStock(Long activityId);

    int recoverStock(Long activityId);

}
