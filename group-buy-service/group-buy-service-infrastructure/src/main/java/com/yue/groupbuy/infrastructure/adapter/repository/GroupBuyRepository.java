package com.yue.groupbuy.infrastructure.adapter.repository;

import com.yue.groupbuy.domain.trade.adapter.repository.IGroupBuyRepository;
import com.yue.groupbuy.domain.trade.model.entity.ActivityPricingEntity;
import com.yue.groupbuy.domain.trade.model.entity.LockOrderCommand;
import com.yue.groupbuy.domain.trade.model.entity.LockOrderResultEntity;
import com.yue.groupbuy.domain.trade.model.valobj.GroupBuyOrderStatusVO;
import com.yue.groupbuy.infrastructure.dao.*;
import com.yue.groupbuy.infrastructure.dao.po.*;
import com.yue.groupbuy.types.enums.ResponseCode;
import com.yue.groupbuy.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Repository
public class GroupBuyRepository implements IGroupBuyRepository {

    @Resource
    private IGroupBuyActivityDao activityDao;
    @Resource
    private IDiscountDao discountDao;
    @Resource
    private ISkuDao skuDao;
    @Resource
    private ICategoryDao categoryDao;
    @Resource
    private ISkuResourceDetailDao skuResourceDetailDao;
    @Resource
    private IGroupBuyOrderDao groupBuyOrderDao;
    @Resource
    private ITOrderGroupDao tOrderGroupDao;

    @Override
    public ActivityPricingEntity queryActivityPricing(Long activityId, String goodsId) {
        GroupBuyActivity activity = activityDao.queryByActivityId(activityId);
        if (activity == null || activity.getStatus() != 1) {
            return null;
        }

        Sku sku = skuDao.queryByGoodsId(goodsId);
        if (sku == null) {
            return null;
        }

        BigDecimal originalPrice = sku.getOriginalPrice();
        BigDecimal payPrice = computePayPrice(activity.getDiscountId(), originalPrice);
        BigDecimal deductionPrice = originalPrice.subtract(payPrice);

        return ActivityPricingEntity.builder()
                .activityId(activityId)
                .activityName(activity.getActivityName())
                .target(activity.getTarget())
                .validTime(activity.getValidTime())
                .takeLimitCount(activity.getTakeLimitCount())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .originalPrice(originalPrice)
                .deductionPrice(deductionPrice)
                .payPrice(payPrice)
                .build();
    }

    private BigDecimal computePayPrice(String discountId, BigDecimal originalPrice) {
        if (StringUtils.isBlank(discountId)) {
            return originalPrice;
        }
        Discount discount = discountDao.queryByDiscountId(discountId);
        if (discount == null || StringUtils.isBlank(discount.getMarketPlan())) {
            return originalPrice;
        }
        String plan = discount.getMarketPlan().trim().toUpperCase();
        String expr = discount.getMarketExpr();
        try {
            switch (plan) {
                case "ZJ": // 直减
                    return ensurePositive(originalPrice.subtract(new BigDecimal(expr.trim())));
                case "MJ": { // 满减 "100,10"
                    String[] parts = expr.split(",");
                    BigDecimal threshold = new BigDecimal(parts[0].trim());
                    BigDecimal reduction = new BigDecimal(parts[1].trim());
                    if (originalPrice.compareTo(threshold) >= 0) {
                        return ensurePositive(originalPrice.subtract(reduction));
                    }
                    return originalPrice;
                }
                case "ZK": // 折扣（0.9 = 九折）
                    return ensurePositive(originalPrice.multiply(new BigDecimal(expr.trim())));
                case "N": // N元购
                    return new BigDecimal(expr.trim());
                default:
                    return originalPrice;
            }
        } catch (Exception e) {
            log.warn("折扣计算异常 discountId:{} plan:{} expr:{}", discountId, plan, expr, e);
            return originalPrice;
        }
    }

    private BigDecimal ensurePositive(BigDecimal price) {
        return price.compareTo(BigDecimal.ZERO) <= 0 ? new BigDecimal("0.01") : price;
    }

    @Override
    public int queryUserOrderCount(String goodsId, String userId) {
        return tOrderGroupDao.queryUserOrderCount(goodsId, userId);
    }

    @Override
    public int queryTeamLockCount(String teamId) {
        GroupBuyOrder order = groupBuyOrderDao.queryGroupBuyProgress(teamId);
        return order == null ? 0 : order.getLockCount();
    }

    @Override
    @Transactional(timeout = 5)
    public LockOrderResultEntity lockGroupBuyOrder(LockOrderCommand command) {
        String teamId = command.getTeamId();
        boolean newTeam = StringUtils.isBlank(teamId);

        if (newTeam) {
            teamId = RandomStringUtils.randomNumeric(8);
            Date now = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(now);
            cal.add(Calendar.MINUTE, command.getValidTime());

            GroupBuyOrder groupBuyOrder = GroupBuyOrder.builder()
                    .teamId(teamId)
                    .activityId(command.getActivityId())
                    .source(command.getSource())
                    .channel(command.getChannel())
                    .originalPrice(command.getOriginalPrice())
                    .deductionPrice(command.getDeductionPrice())
                    .payPrice(command.getPayPrice())
                    .targetCount(command.getTargetCount())
                    .completeCount(0)
                    .lockCount(1)
                    .validStartTime(now)
                    .validEndTime(cal.getTime())
                    .notifyType("MQ")
                    .build();

            groupBuyOrderDao.insert(groupBuyOrder);
        } else {
            int updated = groupBuyOrderDao.updateAddLockCount(teamId);
            if (updated != 1) {
                throw new AppException(ResponseCode.TEAM_FULL);
            }
        }

        return LockOrderResultEntity.builder()
                .teamId(teamId)
                .newTeam(newTeam)
                .build();
    }

    @Override
    @Transactional(timeout = 5)
    public void saveOrderGroup(String orderId, String teamId, Long activityId, Date startTime, Date endTime) {
        TOrderGroup tOrderGroup = TOrderGroup.builder()
                .orderId(orderId)
                .teamId(teamId)
                .activityId(activityId)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        try {
            tOrderGroupDao.insert(tOrderGroup);
        } catch (DuplicateKeyException e) {
            log.warn("t_order_group 重复插入，忽略 orderId:{}", orderId);
        }
    }

    @Override
    public void compensateLockCount(String teamId, boolean newTeam) {
        if (newTeam) {
            // 新建队伍失败：理论上不会走到这里（新建后调 order-service 失败），
            // 但仍然把 lockCount 减回去（team 还在，但 lockCount = 0 状态不可用，等自然过期）
            groupBuyOrderDao.updateSubLockCount(teamId);
        } else {
            groupBuyOrderDao.updateSubLockCount(teamId);
        }
    }

    @Override
    public Map<String, Object> queryTeamInfoByOutTradeNo(String outTradeNo) {
        return tOrderGroupDao.queryTeamInfoByOutTradeNo(outTradeNo);
    }

    @Override
    @Transactional(timeout = 5)
    public boolean settleGroupBuyOrder(String teamId) {
        GroupBuyOrder progress = groupBuyOrderDao.queryGroupBuyProgress(teamId);
        if (progress == null) {
            log.warn("settleGroupBuyOrder：group_buy_order 不存在 teamId:{}", teamId);
            return false;
        }

        int updated = groupBuyOrderDao.updateAddCompleteCount(teamId);
        if (updated != 1) {
            log.warn("settleGroupBuyOrder：updateAddCompleteCount 未命中，可能已结算 teamId:{}", teamId);
            return false;
        }

        // 判断是否达成目标（当前 completeCount 是更新前的值）
        boolean complete = (progress.getCompleteCount() + 1) >= progress.getTargetCount();
        if (complete) {
            groupBuyOrderDao.updateOrderStatus2Complete(teamId);
        }
        return complete;
    }

    @Override
    public List<Map<String, Object>> queryCompletedOrdersForTeam(String teamId) {
        List<Map<String, Object>> orders = tOrderGroupDao.queryCompletedOrdersForTeam(teamId);
        if (orders == null) {
            return Collections.emptyList();
        }
        // 补充 goodsType / res_key / res_value
        for (Map<String, Object> order : orders) {
            String goodsId = (String) order.get("goodsId");
            if (StringUtils.isNotBlank(goodsId)) {
                Sku sku = skuDao.queryByGoodsId(goodsId);
                if (sku != null && sku.getCategoryId() != null) {
                    Category cat = categoryDao.queryById(sku.getCategoryId());
                    if (cat != null) {
                        order.put("goodsType", cat.getCode());
                    }
                }
                List<SkuResourceDetail> resources = skuResourceDetailDao.listByGoodsId(goodsId);
                if (resources != null && !resources.isEmpty()) {
                    order.put("resKey", resources.get(0).getResKey());
                    order.put("resValue", resources.get(0).getResValue());
                }
            }
        }
        return orders;
    }

    @Override
    public String queryTeamIdByOrder(String userId, String orderId) {
        return tOrderGroupDao.queryTeamIdByOrder(userId, orderId);
    }

    @Override
    public GroupBuyOrderStatusVO queryTeamStatus(String teamId) {
        GroupBuyOrder order = groupBuyOrderDao.queryGroupBuyTeamByTeamId(teamId);
        if (order == null) return null;
        return GroupBuyOrderStatusVO.valueOf(order.getStatus());
    }

    @Override
    @Transactional(timeout = 5)
    public void refundGroupBuyOrder(String teamId, boolean paid) {
        if (paid) {
            groupBuyOrderDao.refundPaid(teamId);
        } else {
            groupBuyOrderDao.refundUnpaid(teamId);
        }
    }
}
