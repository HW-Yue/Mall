package com.permissionsystem.service;

import com.permissionsystem.dto.PageResult;
import com.permissionsystem.dto.RoleDTO;
import com.permissionsystem.entity.Role;

import java.util.List;
import java.util.Set;

public interface RoleService {
    PageResult page(Integer page, Integer pageSize,String name);

    void add(RoleDTO role);

    RoleDTO getById(Integer id);



    void update(RoleDTO role);

    void delete(String ids);

    /**
     * 创建或更新角色的方法
     * @param roleDTO 从前端接收的数据
     * @return 保存后的 Role 实体
     */
    Role createOrUpdateRole(RoleDTO roleDTO);

    List<String> getRoleNamesByRoleIds(List<Integer> roleIds);

    List<String> getPermissionNamesByPermissionIds(Set<Integer> permissions);
}
