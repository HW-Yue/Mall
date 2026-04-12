package com.permissionsystem.dto;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
/**
 * 用户数据传输对象 (DTO)
 * 用于在程序各层之间传递用户信息，或作为返回给前端的视图对象 (VO)
 */
@Data // Lombok注解，自动生成-getter, setter, toString, equals, hashCode等方法
@Description("用于传输用户信息的类")
public class UserDTO {

    @Description("用户ID")
    private Integer id;
    @Description("用户UUID")
    private String uuid;
    @Description("用户名")
    private String username;
    @Description("姓名")
    private String name;
    @Description("邮箱")
    private String email;
    @Description("密码")
    private String password; // 注意：在返回给前端时，通常会擦除此字段

    @Description("创建时间")
    private LocalDateTime createdAt;

    @Description("更新时间")
    private LocalDateTime updatedAt;

    /**
     * 封装该用户拥有的角色信息
     * 这是在业务逻辑层(Service)中组装的数据，而非直接来自 'users' 表
     */
    @Description("用户拥有的角色信息")
    private Set<RoleDTO> roles = new HashSet<>();


}