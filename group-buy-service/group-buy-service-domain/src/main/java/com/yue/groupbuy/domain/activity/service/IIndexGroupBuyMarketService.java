package com.yue.groupbuy.domain.activity.service;

import com.yue.groupbuy.domain.activity.model.entity.GroupBuyGoodsEntity;
import com.yue.groupbuy.domain.activity.model.entity.MarketProductEntity;
import com.yue.groupbuy.domain.activity.model.entity.TrialBalanceEntity;
import com.yue.groupbuy.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.yue.groupbuy.domain.activity.model.valobj.TeamStatisticVO;

import java.util.List;

public interface IIndexGroupBuyMarketService {

    TrialBalanceEntity indexMarketTrial(MarketProductEntity marketProductEntity) throws Exception;

    /**
     * 查询进行中的拼团订单
     *
     * @param goodsId     商品ID
     * @param userId      用户ID
     * @param ownerCount  个人数量
     * @param randomCount 随机数量
     * @return 用户拼团明细数据
     */
    List<UserGroupBuyOrderDetailEntity> queryInProgressUserGroupBuyOrderDetailList(String goodsId, String userId, Integer ownerCount, Integer randomCount);

    /**
     * 商品拼团队伍总结
     *
     * @param goodsId 商品ID
     * @return 队伍统计
     */
    TeamStatisticVO queryTeamStatisticByGoodsId(String goodsId);

    /**
     * 查询拼团商品列表
     */
    List<GroupBuyGoodsEntity> queryGroupBuyGoodsList();

}
