package cn.bugstack.ai.infrastructure.dao;

import cn.bugstack.ai.infrastructure.dao.po.AiResourceDeliveryLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI资源下发/充值流水表 DAO
 */
@Mapper
public interface IAiResourceDeliveryLogDao {

    int insert(AiResourceDeliveryLog record);

    AiResourceDeliveryLog queryByOrderId(String orderId);

    List<AiResourceDeliveryLog> queryByUserId(String userId);

    List<AiResourceDeliveryLog> queryPageByUserId(@Param("userId") String userId,
                                                  @Param("bizType") String bizType,
                                                  @Param("status") Integer status,
                                                  @Param("offset") Integer offset,
                                                  @Param("limit") Integer limit);

    long countByUserId(@Param("userId") String userId,
                       @Param("bizType") String bizType,
                       @Param("status") Integer status);

    int updateStatusByOrderId(String orderId, Integer status);
}

