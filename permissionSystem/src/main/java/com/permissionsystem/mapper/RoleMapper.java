package com.permissionsystem.mapper;

import com.permissionsystem.dto.RoleDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Set;

@Mapper
public interface RoleMapper {


    List<RoleDTO> listRole(String name);


    @Insert("insert into role(name, uuid, created_at, updated_at) values(#{name}, #{uuid}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    void add(RoleDTO role);


    void addRolePermission(@Param("roleId") Integer id,@Param("permissions") List<Integer> permissions);

    //根据id查询角色的权限
    RoleDTO getById(@Param("id") Integer id);

    void deleteRolePermissionById(@Param("roleId") Integer id);


    void deleteRoleByIds( @Param("ids")int[] idsInteger);

    void deleteRolePermissionByIds(@Param("ids") int[] idsInteger);


    List<String> getRoleNamesByRoleIds(@Param("roleIds") List<Integer> roleIds);

    List<String> getPermissionNamesByPermissionIds(@Param("permissions") Set<Integer> permissions);

    void updateRole(RoleDTO role);
}
