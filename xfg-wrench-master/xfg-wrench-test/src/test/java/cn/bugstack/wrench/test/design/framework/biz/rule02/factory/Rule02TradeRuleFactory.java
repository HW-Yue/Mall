package cn.bugstack.wrench.test.design.framework.biz.rule02.factory;

import cn.bugstack.wrench.design.framework.link.model2.DynamicContext;
import cn.bugstack.wrench.design.framework.link.model2.LinkArmory;
import cn.bugstack.wrench.design.framework.link.model2.chain.BusinessLinkedList;
import cn.bugstack.wrench.test.design.framework.biz.rule02.logic.RuleLogic201;
import cn.bugstack.wrench.test.design.framework.biz.rule02.logic.RuleLogic202;
import cn.bugstack.wrench.test.design.framework.biz.rule02.logic.RuleLogic203;
import cn.bugstack.wrench.test.design.framework.biz.rule02.logic.XxxResponse;
import lombok.*;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description
 * @create 2025-01-18 09:19
 */
@Service
public class Rule02TradeRuleFactory {

    @Bean("demo01")
    public BusinessLinkedList<String, DynamicContext, XxxResponse> demo01(RuleLogic201 ruleLogic201,
                                                                          RuleLogic202 ruleLogic202,
                                                                          RuleLogic203 ruleLogic203) {

        LinkArmory<String, DynamicContext, XxxResponse> linkArmory = new LinkArmory<>("demo01", ruleLogic201, ruleLogic202, ruleLogic203);

        return linkArmory.getLogicLink();
    }

    @Bean("demo02")
    public BusinessLinkedList<String, DynamicContext, XxxResponse> demo02(RuleLogic202 ruleLogic202, RuleLogic203 ruleLogic203) {

        LinkArmory<String, DynamicContext, XxxResponse> linkArmory = new LinkArmory<>("demo02", ruleLogic202, ruleLogic203);

        return linkArmory.getLogicLink();
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext extends cn.bugstack.wrench.design.framework.link.model2.DynamicContext {
        private String age;
    }

}
