package com.permissionsystem.controller;

import com.permissionsystem.dto.PageResult;
import com.permissionsystem.dto.Result;
import com.permissionsystem.dto.RoleDTO;
import com.permissionsystem.service.RoleService;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/roles")
public class RoleController {

    @Autowired
    RoleService roleService;
    //1. 角色列表查询
    @GetMapping
    @PreAuthorize("hasAuthority('9')")
    public Result page(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue="10") Integer pageSize,
                        String name){
        log.info(" 查询用户列表,page: {}, pageSize: {} ,昵称:{}", page, pageSize,name);

        PageResult PageResult = roleService.page(page, pageSize,name);
        return Result.success(PageResult);
    }

    //2. 添加角色
    @PostMapping
    @PreAuthorize("hasAuthority('4')")
    public Result add(@RequestBody RoleDTO role){
        log.info(" 添加角色, role: {}", role);
        roleService.add(role);
        return Result.success();
    }

    //3. 修改角色
    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('5')")
    public Result update(@RequestBody RoleDTO role, @PathVariable Integer id){
        role.setId(id);
        log.info(" 修改角色, role: {}", role);
        roleService.update(role);
        return Result.success();
    }
    //4.根据id查询角色
    @PreAuthorize("hasAuthority('9')")
    @GetMapping("{id}")
    public Result get(@PathVariable Integer id){
        log.info("根据id查询角色, id: {}", id);
        RoleDTO role = roleService.getById(id);

        return Result.success(role);
    }

    //5. 删除角色
    @PreAuthorize("hasAuthority('6')")
    @DeleteMapping
    public Result delete(@PathParam("ids") String ids){
        log.info(" 删除角色, ids: {}", ids);

        roleService.delete(ids);
        return Result.success();
    }
}
