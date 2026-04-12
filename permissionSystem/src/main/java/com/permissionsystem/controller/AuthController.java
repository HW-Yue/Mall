package com.permissionsystem.controller;

import com.permissionsystem.dto.JwtResponse;
import com.permissionsystem.dto.LoginRequest;
import com.permissionsystem.service.impl.UserDetailsServiceImpl;
import com.permissionsystem.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// 移除了类级别的 @RequestMapping
@RestController
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtUtils jwtUtils;

    // 将接口路径直接定义在这里
    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody LoginRequest loginRequest) throws Exception {
        // --- 新增的调试日志 ---
        System.out.println("=============================================");
        System.out.println("AuthController: /login 接口被调用");
        if (loginRequest == null) {
            System.out.println("AuthController: 接收到的 LoginRequest 对象为 null！");
        } else {
            System.out.println("AuthController: 收到的 username 是: '" + loginRequest.getUsername() + "'");
            System.out.println("AuthController: 收到的 password 是: '" + loginRequest.getPassword() + "'");
        }
        System.out.println("=============================================");

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            // 为了安全，不要在生产环境中抛出过于详细的异常
            throw new Exception("用户名或密码不正确", e);
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());
        final String jwt = jwtUtils.generateToken(userDetails);

        return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getUsername()));
    }
}

