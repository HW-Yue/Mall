package com.yue.groupbuy.domain.trade.adapter.port;

import com.yue.groupbuy.domain.trade.model.entity.NotifyTaskEntity;

public interface ITradePort {

    String groupBuyNotify(NotifyTaskEntity notifyTask) throws Exception;

}
