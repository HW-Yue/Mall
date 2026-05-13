package com.yue.groupbuy.infrastructure.adapter.repository;

import com.yue.groupbuy.domain.trade.adapter.repository.ITradeRepository;
import com.yue.groupbuy.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import com.yue.groupbuy.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import com.yue.groupbuy.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import com.yue.groupbuy.domain.trade.model.entity.*;
import com.yue.groupbuy.domain.trade.model.valobj.*;
import com.yue.groupbuy.infrastructure.dao.IGroupBuyActivityDao;
import com.yue.groupbuy.infrastructure.dao.IGroupBuyOrderDao;
import com.yue.groupbuy.infrastructure.dao.INotifyTaskDao;
import com.yue.groupbuy.infrastructure.dao.ITOrderGroupDao;
import com.yue.groupbuy.infrastructure.dao.po.GroupBuyActivity;
import com.yue.groupbuy.infrastructure.dao.po.GroupBuyOrder;
import com.yue.groupbuy.infrastructure.dao.po.NotifyTask;
import com.yue.groupbuy.infrastructure.dao.po.TOrderGroup;
import com.yue.groupbuy.types.common.Constants;
import com.yue.groupbuy.types.enums.ActivityStatusEnumVO;
import com.yue.groupbuy.types.enums.GroupBuyOrderEnumVO;
import com.yue.groupbuy.types.enums.ResponseCode;
import com.yue.groupbuy.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.*;

@Slf4j
@Repository
public class TradeRepository implements ITradeRepository {

    @Resource
    private IGroupBuyActivityDao groupBuyActivityDao;
    @Resource
    private IGroupBuyOrderDao groupBuyOrderDao;
    @Resource
    private ITOrderGroupDao tOrderGroupDao;
    @Resource
    private INotifyTaskDao notifyTaskDao;

    @Value("${app.rocketmq.topic.groupBuySuccessNotify:group-buy-success-notify}")
    private String topic_team_success;

    @Value("${app.rocketmq.topic.groupBuyRefund:group-buy-refund-notify}")
    private String topic_team_refund;

    @Override
    public MarketPayOrderEntity queryMarketPayOrderEntityByOrderId(String userId, String orderId) {
        TOrderGroup res = tOrderGroupDao.queryByUserIdAndOrderId(userId, orderId);
        if (null == res) return null;

        return MarketPayOrderEntity.builder()
                .teamId(res.getTeamId())
                .orderId(res.getOrderId())
                .originalPrice(res.getOriginalPrice())
                .deductionPrice(res.getDeductionPrice())
                .payPrice(res.getPayPrice())
                .tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.valueOf(res.getStatus()))
                .build();
    }

    @Transactional(timeout = 500)
    @Override
    public MarketPayOrderEntity lockMarketPayOrder(GroupBuyOrderAggregate groupBuyOrderAggregate) {
        UserEntity userEntity = groupBuyOrderAggregate.getUserEntity();
        PayActivityEntity payActivityEntity = groupBuyOrderAggregate.getPayActivityEntity();
        PayDiscountEntity payDiscountEntity = groupBuyOrderAggregate.getPayDiscountEntity();
        NotifyConfigVO notifyConfigVO = payDiscountEntity.getNotifyConfigVO();
        Integer userTakeOrderCount = groupBuyOrderAggregate.getUserTakeOrderCount();

        String teamId = payActivityEntity.getTeamId();
        if (StringUtils.isBlank(teamId)) {
            teamId = RandomStringUtils.randomNumeric(8);
            Date currentDate = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(currentDate);
            calendar.add(Calendar.MINUTE, payActivityEntity.getValidTime());

            GroupBuyOrder groupBuyOrder = GroupBuyOrder.builder()
                    .teamId(teamId)
                    .activityId(payActivityEntity.getActivityId())
                    .source(payDiscountEntity.getSource())
                    .channel(payDiscountEntity.getChannel())
                    .originalPrice(payDiscountEntity.getOriginalPrice())
                    .deductionPrice(payDiscountEntity.getDeductionPrice())
                    .payPrice(payDiscountEntity.getPayPrice())
                    .targetCount(payActivityEntity.getTargetCount())
                    .completeCount(0)
                    .lockCount(1)
                    .validStartTime(currentDate)
                    .validEndTime(calendar.getTime())
                    .notifyType(notifyConfigVO != null ? notifyConfigVO.getNotifyType() : NotifyTypeEnumVO.MQ.getCode())
                    .notifyUrl(notifyConfigVO != null ? notifyConfigVO.getNotifyUrl() : null)
                    .build();
            groupBuyOrderDao.insert(groupBuyOrder);
        } else {
            int updateCount = groupBuyOrderDao.updateAddLockCount(teamId);
            if (1 != updateCount) {
                throw new AppException(ResponseCode.E0005);
            }
        }

        return MarketPayOrderEntity.builder()
                .originalPrice(payDiscountEntity.getOriginalPrice())
                .deductionPrice(payDiscountEntity.getDeductionPrice())
                .payPrice(payDiscountEntity.getPayPrice())
                .tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.CREATE)
                .teamId(teamId)
                .build();
    }

    @Override
    public GroupBuyProgressVO queryGroupBuyProgress(String teamId) {
        GroupBuyOrder order = groupBuyOrderDao.queryGroupBuyProgress(teamId);
        if (null == order) return null;
        return GroupBuyProgressVO.builder()
                .completeCount(order.getCompleteCount())
                .targetCount(order.getTargetCount())
                .lockCount(order.getLockCount())
                .build();
    }

    @Override
    public GroupBuyActivityEntity queryGroupBuyActivityEntityByActivityId(Long activityId) {
        GroupBuyActivity activity = groupBuyActivityDao.queryByActivityId(activityId);
        if (null == activity) return null;
        return GroupBuyActivityEntity.builder()
                .activityId(activity.getActivityId())
                .activityName(activity.getActivityName())
                .groupType(activity.getGroupType())
                .takeLimitCount(activity.getTakeLimitCount())
                .target(activity.getTarget())
                .validTime(activity.getValidTime())
                .status(ActivityStatusEnumVO.valueOf(activity.getStatus()))
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .tagId(activity.getTagId())
                .tagScope(activity.getTagScope())
                .build();
    }

    @Override
    public Integer queryOrderCountByGoodsId(String goodsId, String userId) {
        return tOrderGroupDao.queryUserOrderCount(goodsId, userId);
    }

    @Override
    public GroupBuyTeamEntity queryGroupBuyTeamByTeamId(String teamId) {
        GroupBuyOrder order = groupBuyOrderDao.queryGroupBuyTeamByTeamId(teamId);
        if (null == order) return null;
        return GroupBuyTeamEntity.builder()
                .teamId(order.getTeamId())
                .activityId(order.getActivityId())
                .targetCount(order.getTargetCount())
                .completeCount(order.getCompleteCount())
                .lockCount(order.getLockCount())
                .status(GroupBuyOrderEnumVO.valueOf(order.getStatus()))
                .validStartTime(order.getValidStartTime())
                .validEndTime(order.getValidEndTime())
                .notifyConfigVO(NotifyConfigVO.builder()
                        .notifyType(order.getNotifyType())
                        .notifyUrl(order.getNotifyUrl())
                        .notifyMQ(topic_team_success)
                        .build())
                .build();
    }

    @Transactional(timeout = 5000)
    @Override
    public NotifyTaskEntity settlementMarketPayOrder(GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate) {
        UserEntity userEntity = groupBuyTeamSettlementAggregate.getUserEntity();
        GroupBuyTeamEntity groupBuyTeamEntity = groupBuyTeamSettlementAggregate.getGroupBuyTeamEntity();
        NotifyConfigVO notifyConfigVO = groupBuyTeamEntity.getNotifyConfigVO();
        TradePaySuccessEntity tradePaySuccessEntity = groupBuyTeamSettlementAggregate.getTradePaySuccessEntity();

        int updateOrderCount = tOrderGroupDao.updateStatus2Complete(
                tradePaySuccessEntity.getOrderId(), tradePaySuccessEntity.getOutTradeTime());
        if (1 != updateOrderCount) {
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        int updateAddCount = groupBuyOrderDao.updateAddCompleteCount(groupBuyTeamEntity.getTeamId());
        if (1 != updateAddCount) {
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        if (groupBuyTeamEntity.getTargetCount() - groupBuyTeamEntity.getCompleteCount() == 1) {
            int updateStatusCount = groupBuyOrderDao.updateOrderStatus2COMPLETE(groupBuyTeamEntity.getTeamId());
            if (1 != updateStatusCount) {
                throw new AppException(ResponseCode.UPDATE_ZERO);
            }

            List<String> orderIdList = tOrderGroupDao.queryCompleteOrderIdListByTeamId(groupBuyTeamEntity.getTeamId());

            NotifyTask notifyTask = new NotifyTask();
            notifyTask.setActivityId(groupBuyTeamEntity.getActivityId());
            notifyTask.setTeamId(groupBuyTeamEntity.getTeamId());
            notifyTask.setNotifyCategory(TaskNotifyCategoryEnumVO.TRADE_SETTLEMENT.getCode());
            notifyTask.setNotifyType(notifyConfigVO.getNotifyType());
            notifyTask.setNotifyMQ(NotifyTypeEnumVO.MQ.getCode().equals(notifyConfigVO.getNotifyType()) ? notifyConfigVO.getNotifyMQ() : null);
            notifyTask.setNotifyUrl(NotifyTypeEnumVO.HTTP.getCode().equals(notifyConfigVO.getNotifyType()) ? notifyConfigVO.getNotifyUrl() : null);
            notifyTask.setNotifyCount(0);
            notifyTask.setNotifyStatus(0);
            notifyTask.setUuid(groupBuyTeamEntity.getTeamId() + Constants.UNDERLINE + TaskNotifyCategoryEnumVO.TRADE_SETTLEMENT.getCode() + Constants.UNDERLINE + tradePaySuccessEntity.getOrderId());
            notifyTask.setParameterJson(JSON.toJSONString(new HashMap<String, Object>() {{
                put("teamId", groupBuyTeamEntity.getTeamId());
                put("orderIdList", orderIdList);
            }}));
            notifyTaskDao.insert(notifyTask);

            return buildNotifyTaskEntity(notifyTask);
        }

        return null;
    }

    @Override
    public boolean isSCBlackIntercept(String source, String channel) {
        return false;
    }

    @Override
    public List<NotifyTaskEntity> queryUnExecutedNotifyTaskList() {
        List<NotifyTask> list = notifyTaskDao.queryUnExecutedNotifyTaskList();
        if (list == null || list.isEmpty()) return new ArrayList<>();
        List<NotifyTaskEntity> result = new ArrayList<>();
        for (NotifyTask task : list) {
            result.add(buildNotifyTaskEntity(task));
        }
        return result;
    }

    @Override
    public List<NotifyTaskEntity> queryUnExecutedNotifyTaskList(String teamId) {
        NotifyTask task = notifyTaskDao.queryUnExecutedNotifyTaskByTeamId(teamId);
        if (null == task) return new ArrayList<>();
        return Collections.singletonList(buildNotifyTaskEntity(task));
    }

    @Override
    public int updateNotifyTaskStatusSuccess(NotifyTaskEntity entity) {
        return notifyTaskDao.updateNotifyTaskStatusSuccess(NotifyTask.builder()
                .teamId(entity.getTeamId()).uuid(entity.getUuid()).build());
    }

    @Override
    public int updateNotifyTaskStatusError(NotifyTaskEntity entity) {
        return notifyTaskDao.updateNotifyTaskStatusError(NotifyTask.builder()
                .teamId(entity.getTeamId()).uuid(entity.getUuid()).build());
    }

    @Override
    public int updateNotifyTaskStatusRetry(NotifyTaskEntity entity) {
        return notifyTaskDao.updateNotifyTaskStatusRetry(NotifyTask.builder()
                .teamId(entity.getTeamId()).uuid(entity.getUuid()).build());
    }

    @Override
    public boolean occupyTeamStock(String teamStockKey, String recoveryTeamStockKey, Integer target, Integer validTime) {
        log.info("occupyTeamStock teamStockKey:{} target:{}", teamStockKey, target);
        return true;
    }

    @Override
    public void recoveryTeamStock(String recoveryTeamStockKey, Integer validTime) {
        log.info("recoveryTeamStock recoveryTeamStockKey:{}", recoveryTeamStockKey);
    }

    @Transactional(timeout = 5000)
    @Override
    public NotifyTaskEntity unpaid2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate) {
        TradeRefundOrderEntity entity = groupBuyRefundAggregate.getTradeRefundOrderEntity();
        GroupBuyProgressVO progress = groupBuyRefundAggregate.getGroupBuyProgress();

        int updateCount = tOrderGroupDao.update2Refund(entity.getUserId(), entity.getOrderId());
        if (1 != updateCount) {
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        GroupBuyOrder orderReq = new GroupBuyOrder();
        orderReq.setTeamId(entity.getTeamId());
        orderReq.setLockCount(progress.getLockCount());
        int updateTeamCount = groupBuyOrderDao.unpaid2Refund(orderReq);
        if (1 != updateTeamCount) {
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        NotifyTask notifyTask = buildRefundNotifyTask(entity, TaskNotifyCategoryEnumVO.TRADE_UNPAID2REFUND, RefundTypeEnumVO.UNPAID_UNLOCK);
        notifyTaskDao.insert(notifyTask);
        return buildNotifyTaskEntity(notifyTask);
    }

    @Transactional(timeout = 5000)
    @Override
    public NotifyTaskEntity paid2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate) {
        TradeRefundOrderEntity entity = groupBuyRefundAggregate.getTradeRefundOrderEntity();
        GroupBuyProgressVO progress = groupBuyRefundAggregate.getGroupBuyProgress();

        int updateCount = tOrderGroupDao.update2Refund(entity.getUserId(), entity.getOrderId());
        if (1 != updateCount) {
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        GroupBuyOrder orderReq = new GroupBuyOrder();
        orderReq.setTeamId(entity.getTeamId());
        orderReq.setLockCount(progress.getLockCount());
        orderReq.setCompleteCount(progress.getCompleteCount());
        int updateTeamCount = groupBuyOrderDao.paid2Refund(orderReq);
        if (1 != updateTeamCount) {
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        NotifyTask notifyTask = buildRefundNotifyTask(entity, TaskNotifyCategoryEnumVO.TRADE_PAID2REFUND, RefundTypeEnumVO.PAID_UNFORMED);
        notifyTaskDao.insert(notifyTask);
        return buildNotifyTaskEntity(notifyTask);
    }

    @Transactional(timeout = 5000)
    @Override
    public NotifyTaskEntity paidTeam2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate) {
        TradeRefundOrderEntity entity = groupBuyRefundAggregate.getTradeRefundOrderEntity();
        GroupBuyProgressVO progress = groupBuyRefundAggregate.getGroupBuyProgress();
        GroupBuyOrderEnumVO enumVO = groupBuyRefundAggregate.getGroupBuyOrderEnumVO();

        int updateCount = tOrderGroupDao.update2Refund(entity.getUserId(), entity.getOrderId());
        if (1 != updateCount) {
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        GroupBuyOrder orderReq = new GroupBuyOrder();
        orderReq.setTeamId(entity.getTeamId());
        orderReq.setLockCount(progress.getLockCount());
        orderReq.setCompleteCount(progress.getCompleteCount());

        if (GroupBuyOrderEnumVO.COMPLETE_FAIL.equals(enumVO)) {
            int updateTeam = groupBuyOrderDao.paidTeam2Refund(orderReq);
            if (1 != updateTeam) throw new AppException(ResponseCode.UPDATE_ZERO);
        } else if (GroupBuyOrderEnumVO.FAIL.equals(enumVO)) {
            int updateTeam = groupBuyOrderDao.paidTeam2RefundFail(orderReq);
            if (1 != updateTeam) throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        NotifyTask notifyTask = buildRefundNotifyTask(entity, TaskNotifyCategoryEnumVO.TRADE_PAID_TEAM2REFUND, RefundTypeEnumVO.PAID_FORMED);
        notifyTaskDao.insert(notifyTask);
        return buildNotifyTaskEntity(notifyTask);
    }

    @Override
    public void refund2AddRecovery(String recoveryTeamStockKey, String orderId) {
        log.info("refund2AddRecovery recoveryTeamStockKey:{} orderId:{}", recoveryTeamStockKey, orderId);
    }

    @Override
    public int updateTeamStatus2Fail(String teamId) {
        return groupBuyOrderDao.updateOrderStatus2Fail(teamId);
    }

    @Override
    public List<TeamOrderEntity> queryPaidOrdersByTeamId(String teamId) {
        List<TOrderGroup> list = tOrderGroupDao.queryPaidOrdersByTeamId(teamId);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        List<TeamOrderEntity> result = new ArrayList<>(list.size());
        for (TOrderGroup po : list) {
            result.add(TeamOrderEntity.builder()
                    .orderId(po.getOrderId())
                    .userId(po.getUserId())
                    .status(po.getStatus())
                    .build());
        }
        return result;
    }

    @Override
    public List<TeamOrderEntity> queryUnpaidOrdersByTeamId(String teamId) {
        List<TOrderGroup> list = tOrderGroupDao.queryUnpaidOrdersByTeamId(teamId);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        List<TeamOrderEntity> result = new ArrayList<>(list.size());
        for (TOrderGroup po : list) {
            result.add(TeamOrderEntity.builder()
                    .orderId(po.getOrderId())
                    .userId(po.getUserId())
                    .status(po.getStatus())
                    .build());
        }
        return result;
    }

    @Override
    public int closeUnpaidOrdersByTeamId(String teamId) {
        return tOrderGroupDao.closeUnpaidOrdersByTeamId(teamId);
    }

    @Override
    public int updateOrder2Refund(String userId, String orderId) {
        return tOrderGroupDao.update2Refund(userId, orderId);
    }

    @Override
    public int updateOrder2Refunded(String orderId) {
        return tOrderGroupDao.update2Refunded(orderId);
    }

    @Transactional(timeout = 3000)
    @Override
    public boolean closeUnpaidOrderAndReleaseStock(String orderId) {
        int closed = tOrderGroupDao.closeUnpaidByOrderId(orderId);
        if (closed == 0) {
            return false;
        }
        Map<String, Object> teamInfo = tOrderGroupDao.queryTeamInfoByOrderId(orderId);
        if (teamInfo == null || teamInfo.get("teamId") == null) {
            log.warn("拼团关单回退占用库存跳过，未查到 teamId orderId:{}", orderId);
            return true;
        }
        String teamId = String.valueOf(teamInfo.get("teamId"));
        groupBuyOrderDao.updateSubLockCount(teamId);
        return true;
    }

    private NotifyTaskEntity buildNotifyTaskEntity(NotifyTask task) {
        return NotifyTaskEntity.builder()
                .teamId(task.getTeamId())
                .notifyType(task.getNotifyType())
                .notifyMQ(task.getNotifyMQ())
                .notifyUrl(task.getNotifyUrl())
                .notifyCount(task.getNotifyCount())
                .parameterJson(task.getParameterJson())
                .uuid(task.getUuid())
                .build();
    }

    private NotifyTask buildRefundNotifyTask(TradeRefundOrderEntity entity, TaskNotifyCategoryEnumVO category, RefundTypeEnumVO refundType) {
        NotifyTask task = new NotifyTask();
        task.setActivityId(entity.getActivityId());
        task.setTeamId(entity.getTeamId());
        task.setNotifyCategory(category.getCode());
        task.setNotifyType(NotifyTypeEnumVO.MQ.getCode());
        task.setNotifyMQ(topic_team_refund);
        task.setNotifyCount(0);
        task.setNotifyStatus(0);
        task.setUuid(entity.getTeamId() + Constants.UNDERLINE + category.getCode() + Constants.UNDERLINE + entity.getOrderId());
        task.setParameterJson(JSON.toJSONString(new HashMap<String, Object>() {{
            put("type", refundType.getCode());
            put("userId", entity.getUserId());
            put("teamId", entity.getTeamId());
            put("orderId", entity.getOrderId());
            put("activityId", entity.getActivityId());
        }}));
        return task;
    }
}
