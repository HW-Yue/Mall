package com.permissionsystem.mapper;

import com.permissionsystem.dto.LogDTO;
import com.permissionsystem.dto.LogQueryParam;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LogMapper {

    @Insert("insert into sys_log(username, operation_type, method, ip, user_agent, create_date, cost_time) " +
            "values(#{username}, #{operationType}, #{method}, #{ip}, #{userAgent}, #{createDate}, #{costTime})")
    public void insert(LogDTO logDTO);


    List<LogDTO> list(LogQueryParam param);

    void delete(@Param("ids") List<Integer> ids);
}
