package cn.bugstack.wrench.design.framework.tree;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 受理策略处理
 * T 入参类型
 * D 上下文参数
 * R 返参类型
 * @create 2024-12-14 12:06
 */
public interface StrategyHandler<T, D, R> {

    StrategyHandler DEFAULT = (T, D) -> null;

    default R proceed(T requestParameter, D dynamicContext) {
        return null;
    }

    R apply(T requestParameter, D dynamicContext) throws Exception;

}