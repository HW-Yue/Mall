package cn.bugstack.wrench.test.design.framework.biz.rule02.logic;

import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import cn.bugstack.wrench.test.design.framework.biz.rule02.factory.Rule02TradeRuleFactory;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RuleLogic203 implements ILogicHandler<String, Rule02TradeRuleFactory.DynamicContext, XxxResponse> {

    @Override
    public XxxResponse apply(String requestParameter, Rule02TradeRuleFactory.DynamicContext dynamicContext) throws Exception {
        log.info("link model02 RuleLogic203");

//        Integer.parseInt("xxx");

        return stop(requestParameter, dynamicContext, new XxxResponse("hi 小傅哥！"));
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
