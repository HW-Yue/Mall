package com.yue.infrastructure.dao;

import com.yue.infrastructure.dao.po.ActivityType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 活动类型字典 Dao
 */
@Mapper
public interface IActivityTypeDao {

    ActivityType queryActivityTypeById(Integer id);

    ActivityType queryActivityTypeByCode(String typeCode);

    List<ActivityType> queryActivityTypeList();

    /** 查询全部（后台配置用，不筛 status） */
    List<ActivityType> queryActivityTypeListAll();

    int insert(ActivityType po);

    int update(ActivityType po);

    int deleteById(Integer id);
}
