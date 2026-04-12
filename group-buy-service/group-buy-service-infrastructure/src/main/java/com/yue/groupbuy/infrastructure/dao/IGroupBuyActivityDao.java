package com.yue.groupbuy.infrastructure.dao;

import com.yue.groupbuy.infrastructure.dao.po.GroupBuyActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IGroupBuyActivityDao {

    GroupBuyActivity queryByActivityId(@Param("activityId") Long activityId);
}
