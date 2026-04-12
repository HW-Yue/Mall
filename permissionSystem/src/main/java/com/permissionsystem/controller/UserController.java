package com.permissionsystem.controller;


import com.permissionsystem.annotation.LogOperation;
import com.permissionsystem.dto.*;
import com.permissionsystem.service.UserService;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("hasAuthority('8')")
    public Result page(UserQueryParam param){
        log.info(" 查询请求参数",  param);

        PageResult PageResult = userService.page(param);
        return Result.success(PageResult);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('1')")
    public Result add(@RequestBody UserDTO user){
        log.info(" 添加用户, user: {}", user);
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        userService.add(user);
        return Result.success();
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('3')")
    public Result delete(@RequestParam String ids){
        log.info(" 删除用户, ids: {}", ids);

        userService.delete(ids);
        return Result.success();
    }

    //修改用户
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('2')")
    public Result updateUser(@RequestBody UserDTO user, @PathVariable Integer id){
        user.setId(id);
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        log.info(" 修改用户, user: {}", user);
        userService.updateUser(user);
        return Result.success();
    }

    // 添加用户角色
    //将id传给userService
    @PutMapping("{id}/roles")
    @PreAuthorize("hasAuthority('2')")
    public Result addroles(@RequestBody RoleIdsDTO roleIds, @PathVariable Integer id){
        log.info(" 添加用户角色, user: {},roles:{}",id,roleIds.getRoleIds() );

        userService.addRoles(roleIds.getRoleIds(),id);
        return Result.success();
    }
    //获取用户角色
    @GetMapping("{id}/roles")
    @PreAuthorize("hasAuthority('2')")
    public Result getRoles(@PathVariable Integer id){
        log.info(" 获取用户角色, id: {}", id);
        return Result.success(userService.getRoles(id));
    }
}
