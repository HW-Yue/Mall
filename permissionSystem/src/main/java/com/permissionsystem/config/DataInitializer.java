package com.permissionsystem.config;

import com.permissionsystem.entity.Role;
import com.permissionsystem.entity.User;
import com.permissionsystem.repository.RoleRepository;
import com.permissionsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * 这个类会在 Spring Boot 应用启动后自动运行。
 * 它的作用是检查数据库中是否存在测试用户，如果不存在，就创建一个。
 */

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional // 使用事务，确保所有操作要么全部成功，要么全部失败
    public void run(String... args) throws Exception {
        System.out.println("开始执行数据初始化...");
        // 仅为管理员分配一个已存在的角色：ID = 1
        Set<Role> roles = new HashSet<>();
        roleRepository.findById(1).ifPresentOrElse(
                role -> roles.add(role),
                () -> System.out.println("警告: 未在数据库中找到ID为1的角色，管理员将不分配角色。")
        );

        // 初始化用户，并关联角色（若存在）
        createUserIfNotFound("admin", "管理员", "admin@example.com", "123456", roles);
        System.out.println("用户初始化完成。");

        System.out.println("数据初始化全部完成！");
    }

    private void createUserIfNotFound(String username, String name, String email, String rawPassword, Set<Role> roles) {
        userRepository.findByUsername(username).ifPresentOrElse(
                user -> {
                    System.out.println("用户 " + username + " 已存在，仅同步角色为ID=1（若存在），不修改密码，补全UUID...");
                    if (user.getUuid() == null || user.getUuid().isEmpty()) {
                        user.setUuid(java.util.UUID.randomUUID().toString());
                    }
                    if (!roles.isEmpty()) {
                        user.setRoles(roles);
                    }
                    userRepository.save(user);
                },
                () -> {
                    System.out.println("用户 " + username + " 不存在，正在创建...");
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setName(name);
                    newUser.setEmail(email);
                    newUser.setPassword(passwordEncoder.encode(rawPassword));
                    newUser.setUuid(java.util.UUID.randomUUID().toString());
                    newUser.setRoles(roles);
                    userRepository.save(newUser);
                }
        );
    }
}

