package com.permissionsystem.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.permissionsystem.entity.Permission;
import com.permissionsystem.entity.Role;
import com.permissionsystem.mapper.RoleMapper;
import com.permissionsystem.mapper.UserRoleMapper;
import com.permissionsystem.dto.PageResult;
import com.permissionsystem.dto.RoleDTO;
import com.permissionsystem.repository.PermissionRepository;
import com.permissionsystem.repository.RoleRepository;
import com.permissionsystem.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class RoleServiceImpl implements RoleService {
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;


    @Override
    public PageResult page(Integer page, Integer pageSize ,String name) {
        //1.设计分页参数
        PageHelper.startPage(page, pageSize);
        //2.执行查询
        List<RoleDTO> RoleList=roleMapper.listRole(name);
        Page<RoleDTO> p=(Page<RoleDTO>)RoleList;
        //3.封装数据
        return new PageResult(p.getTotal(), p.getResult());
    }

    @Override
    public void add(RoleDTO role) {
        //添加角色到角色表，
        role.setUuid(java.util.UUID.randomUUID().toString());
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.add(role);
        //添加角色和权限的关联关系
        roleMapper.addRolePermission(role.getId(), role.getPermissions());
    }

    //通过角色id查询角色权限
    @Override
    public RoleDTO getById(Integer id) {

        return  roleMapper.getById(id);


    }

    //更新角色权限
    @Transactional
    @Override
    public void update(RoleDTO role) {
        //1.删除角色全部旧权限
        roleMapper.deleteRolePermissionById(role.getId());
        //2.添加角色新的权限，如果权限列表不为空
        if (role.getPermissions() != null && !role.getPermissions().isEmpty()) {
            roleMapper.addRolePermission(role.getId(), role.getPermissions());
        }
        //3.更新角色信息
        roleMapper.updateRole(role);

    }

    @Transactional
    @Override
    public void delete(String ids) {
        //删除角色需要删除角色基本信息，角色权限对应关系，角色用户对应关系
        int[] idsInteger = convertStringToArray(ids);
        roleMapper.deleteRoleByIds(idsInteger);
        roleMapper.deleteRolePermissionByIds(idsInteger);
        userRoleMapper.deleteByRoleIds(idsInteger);
        log.info(" 删除角色, ids: {}", ids);
        log.info(" 删除角色权限, ids: {}", ids);
        log.info(" 删除角色用户, ids: {}", ids);
    }

    // 这个方法可以将一个逗号分隔的字符串转换为整数数组
    public int[] convertStringToArray(String str) {
        if (str == null || str.isEmpty()) {
            return new int[0]; // 如果字符串为空，返回一个空数组
        }

        // 使用 stream 串联操作
        return Arrays.stream(str.split(","))
                .map(String::trim) // 移除可能的空格
                .mapToInt(Integer::parseInt) // 将每个字符串转换为整数
                .toArray();
    }

    //新增角色
    @Override
    @Transactional
    public Role createOrUpdateRole(RoleDTO roleDTO) {
        // 创建一个新的 Role 实体或从数据库中查找现有的 Role
        Role role = new Role();
        role.setName(roleDTO.getName());

        // 处理权限ID列表
        if (roleDTO.getPermissions() != null && !roleDTO.getPermissions().isEmpty()) {
            // 1. 根据前端传入的 permissionIds，从数据库中查询出对应的 Permission 实体列表
            List<Permission> foundPermissions = permissionRepository.findAllById(roleDTO.getPermissions());

            // 2. 将查询到的 List<Permission> 转换为 Set<Permission>
            Set<Permission> permissionsSet = new HashSet<>(foundPermissions);

            // 3. 将权限对象集合设置到 Role 实体中
            role.setPermissions(permissionsSet);
        }

        // 4. 保存 Role 实体到数据库，JPA 会自动处理中间表 role_permissions 的关联关系
        return roleRepository.save(role);
    }

    //通过角色id查询角色名称
    @Override
    public List<String> getRoleNamesByRoleIds(List<Integer> roleIds) {
        return roleMapper.getRoleNamesByRoleIds(roleIds);
    }
    //通过权限id查询权限名称
    @Override
    public List<String> getPermissionNamesByPermissionIds(Set<Integer> permissions) {

        return roleMapper.getPermissionNamesByPermissionIds(permissions);
    }
}
