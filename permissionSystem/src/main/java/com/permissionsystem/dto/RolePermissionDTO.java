package com.permissionsystem.dto;

import lombok.Data;

/**
 * 角色-权限关联关系的数据传输对象 (DTO)
 * 这个类对应于数据库中的 'role_permissions' 中间表的一条记录
 */
@Data
public class RolePermissionDTO {

    /**
     * 角色ID (外键, 对应 roles.id)
     */
    private Integer roleId;

    /**
     * 权限ID (外键, 对应 permissions.id)
     */
    private Integer permissionId;

}