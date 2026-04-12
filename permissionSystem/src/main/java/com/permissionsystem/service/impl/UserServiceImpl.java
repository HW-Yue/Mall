package com.permissionsystem.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.permissionsystem.mapper.UserMapper;
import com.permissionsystem.mapper.UserRoleMapper;
import com.permissionsystem.dto.*;
import com.permissionsystem.service.UserService;
import com.permissionsystem.utils.UserConstants;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;

    @Override
    public LoginInfo login(UserDTO user) {
        UserDTO loginUser = userMapper.getUserByUsernameAndPassword(user);
        if(loginUser != null){
            LoginInfo loginInfo = new LoginInfo(loginUser.getId(), loginUser.getUsername(), loginUser.getName(), "jjj");
            return loginInfo;
        }
        return null;
    }

    @Override
    public PageResult page(UserQueryParam param) {
        //1.设计分页参数
        PageHelper.startPage(param.getPage(), param.getPageSize());
        //2.执行查询
        List<UserDTO> userList=userMapper.list(param);
        Page<UserDTO> p=(Page<UserDTO>)userList;

        //3.封装数据
        return new PageResult(p.getTotal(), p.getResult());

    }

    //添加用户
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(UserDTO user) {
        user.setUuid(UUID.randomUUID().toString());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        //添加用户信息的的时候写不写角色，角色默认为
        userMapper.addUserRole(new UserRoleDTO(user.getId(), UserConstants.USER_ROLE_NORMAL));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String ids) {
        String[] idArray = ids.split(",");
        List<Integer> idsList = new ArrayList<>();
        for (String id : idArray) {
            idsList.add(Integer.parseInt(id));
        }
        userMapper.delete(idsList);
        userRoleMapper.deleteByUserIds(idsList);
    }

    //添加用户角色
    @Override
    public void addRoles(List<Integer> roles,Integer id) {
//        //将用户角色列表从数组中取出，封装成UserRole对象
//        List<UserRoleDTO> userRoleList = new ArrayList<>();
//        for (Integer roleId : roles) {
//            UserRoleDTO userRole = new UserRoleDTO(id, roleId);
//            userRoleList.add(userRole);
//        }
//        //查询数据库，看看哪个角色已经存在了，在添加的时候，忽略掉已经存在的角色
//        List<Integer> roleIds = userRoleMapper.getRoles(id);
//        //todo 这里存在并发问题，如果两个人同时发现某个角色不存在，就会同时插入同一角色，导致数据库报错
//        userRoleList.removeIf(userRole -> roleIds.contains(userRole.getRoleId()));
//        userRoleMapper.addUserRole(userRoleList);
//        //看看哪些角色让删除了,将哪些角色关系删除
//        userRoleMapper.deleteByUserIds(roleIds);
        

        List<Integer> idList = new ArrayList<>();
        idList.add(id);
        // 1. 先删除该用户的所有旧角色
        userRoleMapper.deleteByUserIds(idList);

        // 如果 roles 列表为空，则只删除，不添加新角色
        if (roles == null || roles.isEmpty()) {
            return;
        }

        // 2. 将前端传回的所有新角色一次性插入
        List<UserRoleDTO> userRoleList = new ArrayList<>();
        for (Integer roleId : roles) {
            userRoleList.add(new UserRoleDTO(id, roleId));
        }
        userRoleMapper.addUserRole(userRoleList);

    }

    //查询用户角色列表
    @Override
    public List<Integer> getRoles(Integer id) {
        //获取用户全部角色,封装成List
        return userRoleMapper.getRoles(id);
    }

    @Override
    public void updateUser(UserDTO user) {

        //在数据库里面已经为username和email设置了唯一索引，
//         1. 根据 ID 查询用户，判断记录是否存在
//        User existingUser = userMapper.getById(user.getId());
//        // 2. 如果用户存在，再处理其他业务逻辑
//        // 判断用户名是否已被占用
//        if (userMapper.isUsernameExists(user.getUsername())) {
//            // 注意：这里需要判断，如果用户名是当前用户的，则不报错
//            if (!user.getUsername().equals(existingUser.getUsername())) {
//                throw new ServiceException(ErrorCode.USERNAME_ALREADY_EXISTS);
//            }
//        }
//
//        // 判断邮箱是否已被占用
//        if (userMapper.isEmailExists(user.getEmail())) {
//            if (!user.getEmail().equals(existingUser.getEmail())) {
//                throw new ServiceException(ErrorCode.EMAIL_ALREADY_EXISTS);
//            }
//        }

        // 3. 如果所有判断都通过，最后才执行更新操作
        userMapper.updateUser(user);
    }

    @Override
    public int getIdByUsername(String username) {
        return userMapper.getIdByUsername(username);
    }
}







