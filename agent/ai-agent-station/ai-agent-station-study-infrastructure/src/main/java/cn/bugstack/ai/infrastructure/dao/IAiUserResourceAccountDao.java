package cn.bugstack.ai.infrastructure.dao;

import cn.bugstack.ai.infrastructure.dao.po.AiUserResourceAccount;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户资源账户表 DAO
 */
@Mapper
public interface IAiUserResourceAccountDao {

    int insert(AiUserResourceAccount record);

    int updateByUserAndRes(AiUserResourceAccount record);

    AiUserResourceAccount queryByUserAndRes(String userId, String goodsType, String resKey);

    List<AiUserResourceAccount> queryByUserId(String userId);
}

