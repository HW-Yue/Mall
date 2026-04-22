package cn.bugstack.wrench.test.design.framework.biz.rule02.logic;

import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import cn.bugstack.wrench.test.design.framework.biz.rule02.factory.Rule02TradeRuleFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description
 * @create 2025-01-18 09:18
 */
@Slf4j
@Service
public class RuleLogic201 implements ILogicHandler<String, Rule02TradeRuleFactory.DynamicContext, XxxResponse> {

    public XxxResponse apply(String requestParameter, Rule02TradeRuleFactory.DynamicContext dynamicContext) throws Exception{

        log.info("link model02 RuleLogic201");

        return next(requestParameter, dynamicContext);
    }

}
