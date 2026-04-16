package cn.bugstack.wrench.test.design.framework.biz.rule02.logic;

import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import cn.bugstack.wrench.test.design.framework.biz.rule02.factory.Rule02TradeRuleFactory;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description
 * @create 2025-01-18 09:18
 */
@Slf4j
@Service
public class RuleLogic202 implements ILogicHandler<String, Rule02TradeRuleFactory.DynamicContext, XxxResponse> {

    @Override
    public XxxResponse applyBefore(String requestParameter, Rule02TradeRuleFactory.DynamicContext dynamicContext) throws Exception {
        if ("1".equals(requestParameter)) {
            return jump(requestParameter, dynamicContext, new XxxResponse("00000"));
        } else if ("2".equals(requestParameter)){
            return stop(requestParameter, dynamicContext, new XxxResponse("applyBefore 拦截结果"));
        }
        return next(requestParameter, dynamicContext);
    }

    public XxxResponse apply(String requestParameter, Rule02TradeRuleFactory.DynamicContext dynamicContext) throws Exception {

        log.info("link model02 RuleLogic202");

        return next(requestParameter, dynamicContext);
    }

    @Override
    public void applyAfter(String requestParameter, Rule02TradeRuleFactory.DynamicContext dynamicContext, XxxResponse result) throws Exception {
        log.info("正常结果拦截 {}", JSON.toJSONString(result));
    }

    @Override
    public void applyAfterException(String requestParameter, Rule02TradeRuleFactory.DynamicContext dynamicContext, Exception e) throws Exception {
        log.info("异常结果拦截 {}", e.getMessage());
    }


}
