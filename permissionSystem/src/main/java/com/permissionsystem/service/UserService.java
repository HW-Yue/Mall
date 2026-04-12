package com.permissionsystem.service;

import com.permissionsystem.dto.LoginInfo;
import com.permissionsystem.dto.PageResult;
import com.permissionsystem.dto.UserDTO;
import com.permissionsystem.dto.UserQueryParam;

import java.util.List;

public interface UserService {
    LoginInfo login(UserDTO user);

    PageResult page(UserQueryParam param);

    void add(UserDTO user);

    void delete(String ids);

    void addRoles(List<Integer> roles,Integer id);

    List<Integer> getRoles(Integer id);

    void updateUser(UserDTO user);


    int getIdByUsername(String username);
}
