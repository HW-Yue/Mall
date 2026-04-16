package com.yue.groupbuy.infrastructure.adapter.port;

import com.yue.groupbuy.domain.trade.adapter.port.ITradePort;
import com.yue.groupbuy.domain.trade.model.entity.NotifyTaskEntity;
import com.yue.groupbuy.domain.trade.model.valobj.NotifyTypeEnumVO;
import com.yue.groupbuy.infrastructure.event.GroupBuyEventPublisher;
import com.yue.groupbuy.types.enums.NotifyTaskHTTPEnumVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Slf4j
@Service
public class TradePort implements ITradePort {

    @Resource
    private GroupBuyEventPublisher groupBuyEventPublisher;

    @Override
    public String groupBuyNotify(NotifyTaskEntity notifyTask) throws Exception {
        try {
            // 回调方式 MQ
            if (NotifyTypeEnumVO.MQ.getCode().equals(notifyTask.getNotifyType())) {
                log.info("拼团通知-MQ notifyMQ:{} parameterJson:{}", notifyTask.getNotifyMQ(), notifyTask.getParameterJson());
                groupBuyEventPublisher.publishRawMessage(notifyTask.getNotifyMQ(), notifyTask.getParameterJson());
                return NotifyTaskHTTPEnumVO.SUCCESS.getCode();
            }

            // 回调方式 HTTP（暂不支持，返回成功避免重试）
            if (NotifyTypeEnumVO.HTTP.getCode().equals(notifyTask.getNotifyType())) {
                if (StringUtils.isBlank(notifyTask.getNotifyUrl())) {
                    return NotifyTaskHTTPEnumVO.SUCCESS.getCode();
                }
                log.warn("拼团通知-HTTP 暂不支持，notifyUrl:{}", notifyTask.getNotifyUrl());
                return NotifyTaskHTTPEnumVO.SUCCESS.getCode();
            }

            return NotifyTaskHTTPEnumVO.NULL.getCode();
        } catch (Exception e) {
            log.error("拼团通知异常 teamId:{}", notifyTask.getTeamId(), e);
            return NotifyTaskHTTPEnumVO.ERROR.getCode();
        }
    }

}
