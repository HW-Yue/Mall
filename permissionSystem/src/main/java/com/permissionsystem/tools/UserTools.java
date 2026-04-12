package com.permissionsystem.tools;

import com.permissionsystem.dto.Result;
import com.permissionsystem.dto.RoleDTO;
import com.permissionsystem.mapper.RoleMapper;
import com.permissionsystem.service.impl.RoleServiceImpl;
import com.permissionsystem.service.impl.UserServiceImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class UserTools {
    @Autowired
    UserServiceImpl userService;
    @Autowired
    RoleServiceImpl roleService;
    @Autowired
    RoleMapper roleMapper;
    @Tool("根据账号查询用户角色信息")
    @Transactional
    List<String> getRoles(@P("用户的账号") String username){
        int id = userService.getIdByUsername(username);

        List<Integer> roles = userService.getRoles(id);

        return roleService.getRoleNamesByRoleIds(roles);
    }
    // 根据用户账号查询用户权限
    @Transactional
    @Tool("根据账号查询用户权限信息")
    List<String> getPermissions(@P("用户的账号") String username){
        int id = userService.getIdByUsername(username);

        List<Integer> roles = userService.getRoles(id);
        Set<Integer> permissions = new HashSet();
        for(int role:roles) {
            RoleDTO roleDTO = roleService.getById(role);
            for(int permission:roleDTO.getPermissions()){
                permissions.add(permission);
            }
        }
        return roleService.getPermissionNamesByPermissionIds(permissions);
    }

    @Tool("根据角色名称查询角色权限信息")
    List<String> getPermissionsByRoleName(@P("角色名称") String roleName){
        //1.根据角色名称,查询角色
        List<RoleDTO> roleDTOs = roleMapper.listRole(roleName);
        //2.根据角色id,查询角色权限id
        Set<Integer> permissions = new HashSet<>();
        for(RoleDTO roleDTO:roleDTOs){
            permissions.addAll(roleDTO.getPermissions());
        }
        //3.根据角色权限id,查询角色名称
        return roleService.getPermissionNamesByPermissionIds(permissions);

    }
}
