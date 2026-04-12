package com.permissionsystem.service.impl;

import com.permissionsystem.entity.User;
import com.permissionsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // --- 我们在这里加入了明确的日志 ---
        System.out.println("==========================================================");
        // 改进日志，明确打印出收到的用户名，并用引号括起来以显示空格
        System.out.println("UserDetailsServiceImpl: 正在尝试加载用户，收到的 username 是: '"+ username + "'");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    // --- 用户未找到时的日志 ---
                    System.out.println("UserDetailsServiceImpl: 数据库中未找到用户: '" + username + "'");
                    System.out.println("==========================================================");
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        // --- 用户已找到时的日志 ---
        System.out.println("UserDetailsServiceImpl: 成功从数据库找到用户: " + user.getUsername());
        System.out.println("UserDetailsServiceImpl: 存储的加密密码是: " + user.getPassword());

        // 仅使用固定的权限ID作为授权，不添加任何基于角色的 "ROLE_*" 权限
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getId().toString()))
                .collect(Collectors.toSet());

        System.out.println("UserDetailsServiceImpl: 为用户分配的权限有: " + authorities);
        System.out.println("==========================================================");

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities);
    }

    // 已不再需要基于角色名派生权限的方法，移除以避免误用
}