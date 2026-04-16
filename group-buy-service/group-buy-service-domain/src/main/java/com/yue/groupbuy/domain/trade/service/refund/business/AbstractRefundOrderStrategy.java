package com.yue.groupbuy.domain.trade.service.refund.business;

import com.yue.groupbuy.domain.trade.adapter.repository.ITradeRepository;
import com.yue.groupbuy.domain.trade.model.entity.NotifyTaskEntity;
import com.yue.groupbuy.domain.trade.model.valobj.TeamRefundSuccess;
import com.yue.groupbuy.domain.trade.service.ITradeTaskService;
import com.yue.groupbuy.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import com.yue.groupbuy.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public abstract class AbstractRefundOrderStrategy implements IRefundOrderStrategy {

    @Resource
    protected ITradeRepository repository;

    @Resource
    protected ITradeTaskService tradeTaskService;

    @Resource
    protected ThreadPoolExecutor threadPoolExecutor;

    protected void sendRefundNotifyMessage(NotifyTaskEntity notifyTaskEntity, String refundType) {
        if (null != notifyTaskEntity) {
            threadPoolExecutor.execute(() -> {
                Map<String, Integer> notifyResultMap = null;
                try {
                    notifyResultMap = tradeTaskService.execNotifyJob(notifyTaskEntity);
                    log.info("回调通知交易退单({}) result:{}", refundType, JSON.toJSONString(notifyResultMap));
                } catch (Exception e) {
                    log.error("回调通知交易退单失败({}) result:{}", refundType, JSON.toJSONString(notifyResultMap), e);
                    throw new AppException(e.getMessage());
                }
            });
        }
    }

    protected void doReverseStock(TeamRefundSuccess teamRefundSuccess, String refundType) throws Exception {
        log.info("退单；恢复锁单量 - {} {} {} {}", refundType, teamRefundSuccess.getUserId(), teamRefundSuccess.getActivityId(), teamRefundSuccess.getTeamId());
        String recoveryTeamStockKey = TradeLockRuleFilterFactory.generateRecoveryTeamStockKey(teamRefundSuccess.getActivityId(), teamRefundSuccess.getTeamId());
        repository.refund2AddRecovery(recoveryTeamStockKey, teamRefundSuccess.getOrderId());
    }

}
