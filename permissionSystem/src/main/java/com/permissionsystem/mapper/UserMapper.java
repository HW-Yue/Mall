package com.permissionsystem.mapper;

import com.permissionsystem.dto.UserDTO;
import com.permissionsystem.dto.UserQueryParam;
import com.permissionsystem.dto.UserRoleDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    @Select("select * from user where username = #{username} and password = #{password}")
    UserDTO getUserByUsernameAndPassword(UserDTO user);


    List<UserDTO> list(UserQueryParam param);

    @Insert("insert into user(username, name, email, password, uuid, created_at, updated_at) " +
            "values(#{username}, #{name}, #{email}, #{password}, #{uuid}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    void insert(UserDTO user);

    //添加用户角色
    @Insert("insert into user_role(user_id, role_id) values(#{userId}, #{roleId})")
    void addUserRole(UserRoleDTO userRole);

    //todo 批量删除
    void delete(@Param("ids") List<Integer> ids);

    //更新用户
    @Update("update user set username = #{username}, name = #{name}, email = #{email}, password = #{password}, updated_at = #{updatedAt} where id = #{id}")
    void updateUser(UserDTO user);

    @Select("select * from user where id = #{id}")
    UserDTO getById(Integer id);



    @Select("select * from user where username = #{username}")
    int getIdByUsername(String username);
}
