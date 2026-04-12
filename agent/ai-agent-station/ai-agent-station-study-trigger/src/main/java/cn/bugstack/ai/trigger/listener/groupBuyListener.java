/**
 * @description: 拼团结算成功消息消费者（RocketMQ）
 * @author: 29874
 * @date: 2026/3/16 22:29
 */

package cn.bugstack.ai.trigger.listener;

import cn.bugstack.ai.infrastructure.dao.IAiResourceDeliveryLogDao;
import cn.bugstack.ai.infrastructure.dao.IAiUserResourceAccountDao;
import cn.bugstack.ai.infrastructure.dao.po.AiResourceDeliveryLog;
import cn.bugstack.ai.infrastructure.dao.po.AiUserResourceAccount;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 拼团结算成功回调消息消费者（RocketMQ topic: group-buy-success-notify）
 * 消息示例：
 * {
 *   "orders":[
 *     {"amount":80.00,"goodsType":"ai_model","orderId":"311461351012","res_key":"gpt-4","res_value":"1000000","userId":"xfg702"},
 *     ...
 *   ],
 *   "teamId":"69268465"
 * }
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${app.rocketmq.topic.groupBuySuccessNotify:group-buy-success-notify}",
        consumerGroup = "${app.rocketmq.consumerGroup.groupBuySuccessNotify:CG_AGENT_GROUP_BUY_SUCCESS}"
)
public class groupBuyListener implements RocketMQListener<String> {

    @Resource
    private IAiResourceDeliveryLogDao resourceDeliveryLogDao;

    @Resource
    private IAiUserResourceAccountDao userResourceAccountDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(String paySuccessMessageJson) {
        try {
            log.info("收到组队结算成功消息 {}", paySuccessMessageJson);

            JSONObject root = JSON.parseObject(paySuccessMessageJson);
            String teamId = root.getString("teamId");
            JSONArray orders = root.getJSONArray("orders");

            if (orders == null || orders.isEmpty()) {
                log.warn("组队结算消息中 orders 为空，忽略处理 teamId={}", teamId);
                return;
            }

            for (int i = 0; i < orders.size(); i++) {
                JSONObject order = orders.getJSONObject(i);
                String orderId = order.getString("orderId");
                String userId = order.getString("userId");
                String goodsType = order.getString("goodsType");
                String resKey = order.getString("res_key");
                String resValueStr = order.getString("res_value");

                if (orderId == null || userId == null || goodsType == null || resKey == null || resValueStr == null) {
                    log.warn("订单字段缺失，跳过当前记录 order={}", order);
                    continue;
                }

                BigDecimal resValue = new BigDecimal(resValueStr);

                // 1. 幂等校验：订单是否已经处理过
                AiResourceDeliveryLog existLog = resourceDeliveryLogDao.queryByOrderId(orderId);
                if (existLog != null && existLog.getStatus() != null && existLog.getStatus() == 2) {
                    log.info("订单已处理过，跳过重复消息 orderId={}", orderId);
                    continue;
                }

                // 2. 先插入/更新下发流水表，记录处理中状态
                AiResourceDeliveryLog deliveryLog = AiResourceDeliveryLog.builder()
                        .orderId(orderId)
                        .userId(userId)
                        .bizType("GROUP_BUY")
                        .bizId(teamId)
                        .goodsType(goodsType)
                        .resKey(resKey)
                        .resValue(resValue)
                        .unit("token")
                        .status(1)
                        .build();

                if (existLog == null) {
                    resourceDeliveryLogDao.insert(deliveryLog);
                } else {
                    resourceDeliveryLogDao.updateStatusByOrderId(orderId, 1);
                }

                // 3. 更新/创建用户资源账户
                AiUserResourceAccount account = userResourceAccountDao.queryByUserAndRes(userId, goodsType, resKey);
                if (account == null) {
                    account = AiUserResourceAccount.builder()
                            .userId(userId)
                            .goodsType(goodsType)
                            .resKey(resKey)
                            .totalBalance(resValue)
                            .usedAmount(BigDecimal.ZERO)
                            .frozenAmount(BigDecimal.ZERO)
                            .unit("token")
                            .lastTxId(orderId)
                            .build();
                    userResourceAccountDao.insert(account);
                } else {
                    account.setTotalBalance(account.getTotalBalance().add(resValue));
                    account.setLastTxId(orderId);
                    userResourceAccountDao.updateByUserAndRes(account);
                }

                // 4. 下发成功，更新流水状态为成功
                resourceDeliveryLogDao.updateStatusByOrderId(orderId, 2);
                log.info("组队结算消息处理成功 orderId={}, userId={}, goodsType={}, resKey={}, addValue={}",
                        orderId, userId, goodsType, resKey, resValue);
            }
        } catch (Exception e) {
            log.error("收到支付成功消息失败 {}", paySuccessMessageJson, e);
            throw e;
        }
    }
}
