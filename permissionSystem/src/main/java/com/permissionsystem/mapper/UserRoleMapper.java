package com.permissionsystem.mapper;

import com.permissionsystem.dto.UserRoleDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserRoleMapper {


    // 添加用户角色关系
    void addUserRole(@Param("userRoleList") List<UserRoleDTO> userRoleList);

    List<Integer> getRoles(Integer id);

    void deleteByRoleIds(@Param("ids") int[] idsInteger);

    void deleteByUserIds(@Param("ids") List<Integer> idsList);
}
