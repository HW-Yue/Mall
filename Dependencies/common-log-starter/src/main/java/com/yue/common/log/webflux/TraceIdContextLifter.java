package com.yue.common.log.webflux;

import com.yue.common.log.CommonLogConstants;
import com.yue.common.log.TraceIdContext;
import org.reactivestreams.Subscription;
import org.slf4j.MDC;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Reactor Context 与 MDC 双向同步处理器
 *
 * <p>WebFlux 基于 Reactor 线程模型，请求处理会跨线程，直接使用 MDC.put() 会失效。
 * <p>解决方案：利用 Reactor Context 传递 trace-id，在订阅时同步到 MDC。
 *
 * <p>使用方式：
 * <pre>
 * // 在请求入口处放入 Context
 * Mono.deferContextual(ctx -> {
 *     String traceId = ctx.getOrDefault(TraceIdContextLifter.TRACE_ID_KEY, null);
 *     return mono;
 * })
 * .contextWrite(Context.of(TraceIdContextLifter.TRACE_ID_KEY, traceId))
 * </pre>
 */
public class TraceIdContextLifter<T> implements CoreSubscriber<T> {

    /**
     * Reactor Context 中存储 trace-id 的 key
     */
    public static final String TRACE_ID_KEY = "trace-id";

    private final CoreSubscriber<T> delegate;

    public TraceIdContextLifter(CoreSubscriber<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Context currentContext() {
        return delegate.currentContext();
    }

    @Override
    public void onSubscribe(Subscription s) {
        // 从 Reactor Context 获取 trace-id，放入 MDC
        String traceId = currentContext().getOrDefault(TRACE_ID_KEY, null);
        if (traceId != null) {
            MDC.put(CommonLogConstants.MDC_KEY, traceId);
        }
        delegate.onSubscribe(s);
    }

    @Override
    public void onNext(T t) {
        delegate.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        delegate.onError(t);
    }

    @Override
    public void onComplete() {
        // 清理 MDC，防止线程复用污染
        MDC.clear();
        delegate.onComplete();
    }

    /**
     * 注册全局 Hook，自动为所有 Publisher 添加 ContextLifter
     * 注意：应该在应用启动时调用一次
     */
    @SuppressWarnings("unchecked")
    public static void registerHook() {
        Hooks.onEachOperator(Operators.lift((scannable, subscriber) -> {
            // 只在需要时包装，避免不必要的开销
            if (subscriber instanceof CoreSubscriber) {
                return (CoreSubscriber<Object>) new TraceIdContextLifter<>((CoreSubscriber<?>) subscriber);
            }
            return (CoreSubscriber<Object>) subscriber;
        }));
    }

    /**
     * 重置 Hook（主要用于测试）
     */
    public static void resetHook() {
        Hooks.resetOnEachOperator();
    }

}
