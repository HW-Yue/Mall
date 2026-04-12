package com.yue.groupbuy.infrastructure.adapter.repository;

import com.yue.groupbuy.domain.activity.adapter.repository.IActivityRepository;
import com.yue.groupbuy.domain.activity.model.entity.GroupBuyGoodsEntity;
import com.yue.groupbuy.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.yue.groupbuy.domain.activity.model.valobj.*;
import com.yue.groupbuy.infrastructure.dao.IDiscountDao;
import com.yue.groupbuy.infrastructure.dao.IGroupBuyActivityDao;
import com.yue.groupbuy.infrastructure.dao.IGroupBuyOrderDao;
import com.yue.groupbuy.infrastructure.dao.IScSkuActivityDao;
import com.yue.groupbuy.infrastructure.dao.ISkuDao;
import com.yue.groupbuy.infrastructure.dao.ITOrderGroupDao;
import com.yue.groupbuy.infrastructure.dao.po.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Repository
public class ActivityRepository implements IActivityRepository {

    @Resource
    private IGroupBuyActivityDao groupBuyActivityDao;
    @Resource
    private IDiscountDao discountDao;
    @Resource
    private ISkuDao skuDao;
    @Resource
    private IGroupBuyOrderDao groupBuyOrderDao;
    @Resource
    private ITOrderGroupDao tOrderGroupDao;
    @Resource
    private IScSkuActivityDao scSkuActivityDao;

    @Override
    public GroupBuyActivityDiscountVO queryGroupBuyActivityDiscountVO(Long activityId) {
        GroupBuyActivity activity = groupBuyActivityDao.queryByActivityId(activityId);
        if (null == activity || activity.getStatus() != 1) return null;

        Discount discount = discountDao.queryByDiscountId(activity.getDiscountId());
        if (null == discount) return null;

        GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountName(discount.getDiscountName())
                .discountDesc(discount.getDiscountDesc())
                .discountType(discount.getDiscountType() != null ? DiscountTypeEnum.get(discount.getDiscountType()) : DiscountTypeEnum.BASE)
                .marketPlan(discount.getMarketPlan())
                .marketExpr(discount.getMarketExpr())
                .tagId(discount.getTagId())
                .build();

        return GroupBuyActivityDiscountVO.builder()
                .activityId(activity.getActivityId())
                .activityName(activity.getActivityName())
                .groupBuyDiscount(groupBuyDiscount)
                .groupType(activity.getGroupType())
                .takeLimitCount(activity.getTakeLimitCount())
                .target(activity.getTarget())
                .validTime(activity.getValidTime())
                .status(activity.getStatus())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .tagId(activity.getTagId())
                .tagScope(activity.getTagScope())
                .build();
    }

    @Override
    public SkuVO querySkuByGoodsId(String goodsId) {
        Sku sku = skuDao.queryByGoodsId(goodsId);
        if (null == sku) return null;
        return SkuVO.builder()
                .goodsId(sku.getGoodsId())
                .goodsName(sku.getGoodsName())
                .goodsImageUrl(sku.getGoodsImageUrl())
                .goodsDetail(sku.getGoodsDetail())
                .originalPrice(sku.getOriginalPrice())
                .build();
    }

    @Override
    public SCSkuActivityVO querySCSkuActivityBySCGoodsId(String source, String channel, String goodsId) {
        Long activityId = scSkuActivityDao.queryActivityIdBySCGoodsId(source, channel, goodsId);
        if (activityId == null) return null;
        return SCSkuActivityVO.builder().activityId(activityId).build();
    }

    @Override
    public boolean isTagCrowdRange(String tagId, String userId) {
        // 暂无人群标签 BitSet，默认全部通过
        return true;
    }

    @Override
    public boolean downgradeSwitch() {
        // 暂无 DCC 配置，默认不降级
        return false;
    }

    @Override
    public boolean cutRange(String userId) {
        // 暂无切量配置，默认全量
        return true;
    }

    @Override
    public List<UserGroupBuyOrderDetailEntity> queryInProgressUserGroupBuyOrderDetailListByOwner(String goodsId, String userId, Integer ownerCount) {
        List<Map<String, Object>> rows = tOrderGroupDao.queryInProgressOrdersByActivityAndUser(goodsId, userId, ownerCount);
        if (null == rows || rows.isEmpty()) return null;
        return buildUserGroupBuyOrderDetailEntities(rows);
    }

    @Override
    public List<UserGroupBuyOrderDetailEntity> queryInProgressUserGroupBuyOrderDetailListByRandom(String goodsId, String userId, Integer randomCount) {
        List<Map<String, Object>> rows = tOrderGroupDao.queryInProgressOrdersByActivity(goodsId);
        if (null == rows || rows.isEmpty()) return null;
        if (rows.size() > randomCount) {
            Collections.shuffle(rows);
            rows = rows.subList(0, randomCount);
        }
        return buildUserGroupBuyOrderDetailEntities(rows);
    }

    @Override
    public TeamStatisticVO queryTeamStatisticByGoodsId(String goodsId) {
        List<Map<String, Object>> rows = tOrderGroupDao.queryInProgressOrdersByActivity(goodsId);
        if (null == rows || rows.isEmpty()) {
            return new TeamStatisticVO(0, 0, 0);
        }

        Set<String> teamIds = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String teamId = (String) row.get("teamId");
            if (teamId != null && !teamId.isEmpty()) teamIds.add(teamId);
        }

        if (teamIds.isEmpty()) {
            return new TeamStatisticVO(0, 0, 0);
        }

        Integer allTeamCount = groupBuyOrderDao.queryAllTeamCount(teamIds);
        Integer allTeamCompleteCount = groupBuyOrderDao.queryAllTeamCompleteCount(teamIds);
        Integer allTeamUserCount = groupBuyOrderDao.queryAllUserCount(teamIds);

        return TeamStatisticVO.builder()
                .allTeamCount(allTeamCount)
                .allTeamCompleteCount(allTeamCompleteCount)
                .allTeamUserCount(allTeamUserCount)
                .build();
    }

    private List<UserGroupBuyOrderDetailEntity> buildUserGroupBuyOrderDetailEntities(List<Map<String, Object>> rows) {
        Set<String> teamIds = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String teamId = (String) row.get("teamId");
            if (teamId != null && !teamId.isEmpty()) teamIds.add(teamId);
        }

        if (teamIds.isEmpty()) return null;

        List<GroupBuyOrder> groupBuyOrders = groupBuyOrderDao.queryGroupBuyProgressByTeamIds(teamIds);
        if (null == groupBuyOrders || groupBuyOrders.isEmpty()) return null;

        Map<String, GroupBuyOrder> orderMap = new HashMap<>();
        for (GroupBuyOrder o : groupBuyOrders) {
            orderMap.put(o.getTeamId(), o);
        }

        List<UserGroupBuyOrderDetailEntity> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String teamId = (String) row.get("teamId");
            GroupBuyOrder team = orderMap.get(teamId);
            if (null == team) continue;
            result.add(UserGroupBuyOrderDetailEntity.builder()
                    .userId((String) row.get("userId"))
                    .teamId(team.getTeamId())
                    .activityId(team.getActivityId())
                    .targetCount(team.getTargetCount())
                    .completeCount(team.getCompleteCount())
                    .lockCount(team.getLockCount())
                    .validStartTime(team.getValidStartTime())
                    .validEndTime(team.getValidEndTime())
                    .outTradeNo((String) row.get("outTradeNo"))
                    .build());
        }
        return result;
    }

    @Override
    public List<GroupBuyGoodsEntity> queryGroupBuyGoodsList() {
        List<Sku> skuList = skuDao.queryAllSkuList();
        if (skuList == null || skuList.isEmpty()) {
            return new ArrayList<>();
        }

        List<GroupBuyGoodsEntity> result = new ArrayList<>();
        for (Sku sku : skuList) {
            // 查询商品对应的活动ID
            SCSkuActivityVO scSkuActivityVO = querySCSkuActivityBySCGoodsId("s01", "c01", sku.getGoodsId());
            Long activityId = scSkuActivityVO != null ? scSkuActivityVO.getActivityId() : null;

            // 如果有活动，查询活动折扣计算实际支付价格
            BigDecimal payPrice = sku.getOriginalPrice();
            if (activityId != null) {
                GroupBuyActivityDiscountVO discountVO = queryGroupBuyActivityDiscountVO(activityId);
                if (discountVO != null && discountVO.getGroupBuyDiscount() != null) {
                    // 根据折扣计算支付价格，这里简化处理，实际应根据折扣类型计算
                    payPrice = calculatePayPrice(sku.getOriginalPrice(), discountVO.getGroupBuyDiscount());
                }
            }

            result.add(GroupBuyGoodsEntity.builder()
                    .goodsId(sku.getGoodsId())
                    .goodsName(sku.getGoodsName())
                    .goodsImageUrl(sku.getGoodsImageUrl())
                    .originalPrice(sku.getOriginalPrice())
                    .payPrice(payPrice)
                    .source("s01")
                    .channel("c01")
                    .activityId(activityId)
                    .build());
        }
        return result;
    }

    private BigDecimal calculatePayPrice(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount discount) {
        // 简化处理：直减类型
        if ("ZJ".equals(discount.getMarketPlan()) && discount.getMarketExpr() != null) {
            try {
                BigDecimal deduction = new BigDecimal(discount.getMarketExpr());
                return originalPrice.subtract(deduction).max(BigDecimal.ZERO);
            } catch (NumberFormatException e) {
                log.warn("折扣表达式解析失败: {}", discount.getMarketExpr());
            }
        }
        return originalPrice;
    }

}
