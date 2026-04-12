package com.yue.groupbuy.domain.trade.service;

import com.yue.groupbuy.domain.trade.model.entity.NotifyTaskEntity;

import java.util.Map;

public interface ITradeTaskService {

    Map<String, Integer> execNotifyJob() throws Exception;

    Map<String, Integer> execNotifyJob(String teamId) throws Exception;

    Map<String, Integer> execNotifyJob(NotifyTaskEntity notifyTaskEntity) throws Exception;

}
