package com.yue.groupbuy.infrastructure.dao;

import com.yue.groupbuy.infrastructure.dao.po.GroupBuyOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface IGroupBuyOrderDao {

    void insert(GroupBuyOrder groupBuyOrder);

    /** 加入现有队伍：lockCount + 1（保证 lockCount < targetCount） */
    int updateAddLockCount(@Param("teamId") String teamId);

    /** 补偿：lockCount - 1 */
    void updateSubLockCount(@Param("teamId") String teamId);

    /** 结算：completeCount + 1，返回更新记录数 */
    int updateAddCompleteCount(@Param("teamId") String teamId);

    /** 拼团完成：status = 1 */
    int updateOrderStatus2Complete(@Param("teamId") String teamId);

    /** 拼团完成：status = COMPLETE(1) */
    int updateOrderStatus2COMPLETE(@Param("teamId") String teamId);

    GroupBuyOrder queryGroupBuyProgress(@Param("teamId") String teamId);

    GroupBuyOrder queryGroupBuyTeamByTeamId(@Param("teamId") String teamId);

    /** 退款更新（未支付：lockCount - 1） */
    int refundUnpaid(@Param("teamId") String teamId);

    /** 退款更新（已支付但未成团：lockCount - 1, completeCount - 1） */
    int refundPaid(@Param("teamId") String teamId);

    /** 逆向-未支付退单：lockCount - 1 */
    int unpaid2Refund(GroupBuyOrder groupBuyOrder);

    /** 逆向-已付未成团退单：lockCount - 1, completeCount - 1 */
    int paid2Refund(GroupBuyOrder groupBuyOrder);

    /** 逆向-已付已成团退单（部分退款）：status = COMPLETE_FAIL(3) */
    int paidTeam2Refund(GroupBuyOrder groupBuyOrder);

    /** 逆向-已付已成团退单（全部退款）：status = FAIL(2) */
    int paidTeam2RefundFail(GroupBuyOrder groupBuyOrder);

    List<GroupBuyOrder> queryGroupBuyProgressByTeamIds(@Param("teamIds") Set<String> teamIds);

    Integer queryAllTeamCount(@Param("teamIds") Set<String> teamIds);

    Integer queryAllTeamCompleteCount(@Param("teamIds") Set<String> teamIds);

    Integer queryAllUserCount(@Param("teamIds") Set<String> teamIds);
}
