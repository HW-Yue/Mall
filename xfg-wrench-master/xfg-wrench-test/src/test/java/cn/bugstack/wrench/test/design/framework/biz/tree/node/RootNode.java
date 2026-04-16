package cn.bugstack.wrench.test.design.framework.biz.tree.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.bugstack.wrench.test.design.framework.biz.tree.AbstractXxxSupport;
import cn.bugstack.wrench.test.design.framework.biz.tree.factory.DefaultStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RootNode extends AbstractXxxSupport {

    @Autowired
    private SwitchRoot switchRoot;

    @Override
    protected String doApply(String requestParameter, DefaultStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("【开关节点】规则决策树 userId:{}", requestParameter);
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<String, DefaultStrategyFactory.DynamicContext, String> get(String requestParameter, DefaultStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return switchRoot;
    }

    @Override
    protected void applyAfter(String requestParameter, DefaultStrategyFactory.DynamicContext dynamicContext, String s) {
        log.info("处理完成，applyAfter - 用于做日志、监控和mq处理");
    }

    @Override
    protected void applyAfterException(String requestParameter, DefaultStrategyFactory.DynamicContext dynamicContext, Exception e) {
        log.info("处理异常，applyAfterException - 用于做日志、监控和mq处理" + e.getMessage());
    }

}
