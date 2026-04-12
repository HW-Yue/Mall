package com.permissionsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色数据传输对象 (DTO)
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RoleDTO {

    private Integer id;

    private String uuid;


    private String name;


    private LocalDateTime createdAt;


    private LocalDateTime updatedAt;

    /**
     * 封装该角色拥有的权限信息
     * 这是在业务逻辑层(Service)中组装的数据
     */
    private List<Integer> permissions =  new ArrayList<>();

}
