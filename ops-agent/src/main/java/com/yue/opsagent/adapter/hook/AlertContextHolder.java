package com.yue.opsagent.adapter.hook;

import com.yue.opsagent.domain.model.AlertEvent;

/**
 * 把触发 Agent 的 {@link AlertContext}（告警 + 路由域）透传给 Tool 层。
 *
 * <p>策略层在调用 {@code agent.call()} 前 {@link #set} 当前上下文，执行完毕后
 * {@link #clear}。{@code NacosConfigTool.publishConfig} 在同一线程里读取。</p>
 *
 * <p><b>限制：</b>AgentScope 的 ReActAgent 用 Reactor 异步，跨线程传递依赖
 * {@code InheritableThreadLocal}。V1 假设 tool 在 caller 线程执行，如果 tool 拿到 null，
 * 仍会入 inbox，只是 task 不带原始 alert 与 domain。</p>
 */
public final class AlertContextHolder {

    private static final ThreadLocal<AlertContext> CURRENT = new InheritableThreadLocal<>();

    private AlertContextHolder() {
    }

    public static void set(AlertContext ctx) {
        CURRENT.set(ctx);
    }

    /** 向后兼容旧签名，等价于 {@code set(new AlertContext(event, "manual"))}. */
    public static void set(AlertEvent event) {
        CURRENT.set(new AlertContext(event, "manual"));
    }

    public static AlertContext getContext() {
        return CURRENT.get();
    }

    public static AlertEvent get() {
        AlertContext ctx = CURRENT.get();
        return ctx == null ? null : ctx.event();
    }

    public static String getDomain() {
        AlertContext ctx = CURRENT.get();
        return ctx == null ? null : ctx.domain();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
