package com.permissionsystem.dto;


import lombok.Data;

/**
 * 权限数据传输对象 (DTO)
 */
@Data
public class PermissionDTO {

    private Integer id;

    /**
     * 权限标识符, 例如: "user:create"
     */
    private String name;

    /**
     * 权限的中文描述, 例如: "添加用户"
     */
    private String description;
}

