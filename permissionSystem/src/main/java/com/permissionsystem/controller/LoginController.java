package com.permissionsystem.controller;

import com.permissionsystem.dto.LoginInfo;
import com.permissionsystem.dto.Result;
import com.permissionsystem.dto.UserDTO;
import com.permissionsystem.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class LoginController {

    @Autowired
    private UserService userService;

    //@PostMapping("/login")
    public Result login(@RequestBody UserDTO user){
        log.info("员工来登录啦 , {}", user);
        LoginInfo loginInfo = userService.login(user);
        if(loginInfo != null){
            return Result.success(loginInfo);
        }
        return Result.error("用户名或密码错误~");
    }

}