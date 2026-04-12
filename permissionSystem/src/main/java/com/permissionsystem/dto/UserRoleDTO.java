package com.permissionsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
/**
 * 用户-角色关联关系的数据传输对象 (DTO)
 * 这个类对应于数据库中的 'user_roles' 中间表的一条记录
 */
@Data
public class UserRoleDTO {

    /**
     * 用户ID (外键, 对应 users.id)
     */
    private Integer userId;

    /**
     * 角色ID (外键, 对应 roles.id)
     */
    private Integer roleId;

}