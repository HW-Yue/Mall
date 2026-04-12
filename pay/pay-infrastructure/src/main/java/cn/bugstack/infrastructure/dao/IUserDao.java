package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IUserDao {

    UserAccount queryByOpenid(@Param("openid") String openid);

    UserAccount queryByUsername(@Param("username") String username);

    void insert(UserAccount userAccount);
}
