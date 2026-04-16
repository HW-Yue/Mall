package cn.bugstack.wrench.design.framework.link.model2.handler;

import cn.bugstack.wrench.design.framework.link.model2.DynamicContext;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 逻辑处理器
 * @create 2025-01-18 09:43
 */
public interface ILogicHandler<T, D, R> {

    default R next(T requestParameter, D dynamicContext) {
        if (dynamicContext instanceof DynamicContext) {
            ((DynamicContext) dynamicContext).setJump(false);
            ((DynamicContext) dynamicContext).setProceed(true);
        }
        return null;
    }

    default R stop(T requestParameter, D dynamicContext, R result) {
        if (dynamicContext instanceof DynamicContext) {
            ((DynamicContext) dynamicContext).setJump(false);
            ((DynamicContext) dynamicContext).setProceed(false);
        }
        return result;
    }

    default R jump(T requestParameter, D dynamicContext, R result) {
        if (dynamicContext instanceof DynamicContext) {
            ((DynamicContext) dynamicContext).setJump(true);
            ((DynamicContext) dynamicContext).setProceed(true);
        }
        return result;
    }

    R apply(T requestParameter, D dynamicContext) throws Exception;

    default R applyBefore(T requestParameter, D dynamicContext) throws Exception {
        if (dynamicContext instanceof DynamicContext) {
            ((DynamicContext) dynamicContext).setJump(false);
        }
        return null;
    }

    default void applyAfter(T requestParameter, D dynamicContext, R result) throws Exception {
    }

    default void applyAfterException(T requestParameter, D dynamicContext, Exception e) throws Exception {
    }

}
