package com.yue.groupbuy.domain.activity.adapter.repository;

import com.yue.groupbuy.domain.activity.model.entity.GroupBuyGoodsEntity;
import com.yue.groupbuy.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.yue.groupbuy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.yue.groupbuy.domain.activity.model.valobj.SCSkuActivityVO;
import com.yue.groupbuy.domain.activity.model.valobj.SkuVO;
import com.yue.groupbuy.domain.activity.model.valobj.TeamStatisticVO;

import java.util.List;

public interface IActivityRepository {

    GroupBuyActivityDiscountVO queryGroupBuyActivityDiscountVO(Long activityId);

    SkuVO querySkuByGoodsId(String goodsId);

    SCSkuActivityVO querySCSkuActivityBySCGoodsId(String source, String channel, String goodsId);

    boolean isTagCrowdRange(String tagId, String userId);

    boolean downgradeSwitch();

    boolean cutRange(String userId);

    List<UserGroupBuyOrderDetailEntity> queryInProgressUserGroupBuyOrderDetailListByOwner(String goodsId, String userId, Integer ownerCount);

    List<UserGroupBuyOrderDetailEntity> queryInProgressUserGroupBuyOrderDetailListByRandom(String goodsId, String userId, Integer randomCount);

    TeamStatisticVO queryTeamStatisticByGoodsId(String goodsId);

    /**
     * 查询拼团商品列表
     */
    List<GroupBuyGoodsEntity> queryGroupBuyGoodsList();

}
