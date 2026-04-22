package com.yue.opsagent.adapter.hook;

import com.yue.opsagent.config.ApprovalProperties;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PreActingEvent;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * 审批拦截 Hook（priority=20，Critical 级别，早于业务 hook）。
 *
 * <p>职责：
 * <ul>
 *   <li>{@link PreActingEvent}：对敏感 tool（默认 publishConfig）打审计日志。
 *       <b>不阻止执行</b> —— tool 方法自身会把请求写入 inbox 并抛
 *       {@link io.agentscope.core.tool.ToolSuspendException}，AgentScope 框架会把本次
 *       run 以 {@code TOOL_SUSPENDED} 收尾，同时 inbox 通过 SSE 把新任务推给前端
 *       {@code devops/index.html#inbox}，实现"前端弹框请求同意"。</li>
 *   <li>{@link PostActingEvent}：当 tool 执行被挂起（{@link ToolResultBlock#isSuspended()}），
 *       打一条 "intercepted & suspended" 日志，方便端到端链路追踪。</li>
 * </ul>
 *
 * <p>当前实现不修改 {@code PreActingEvent#setToolUse}，也不主动抛异常 —— 这些都交给
 * tool 层完成。Hook 专注于审计与可观测性，职责清晰。</p>
 */
public class ApprovalInterceptorHook implements Hook {

    private static final Logger log = LoggerFactory.getLogger(ApprovalInterceptorHook.class);

    private final Set<String> sensitiveTools;

    public ApprovalInterceptorHook(ApprovalProperties properties) {
        this.sensitiveTools = Set.copyOf(properties.getSensitiveTools());
    }

    @Override
    public int priority() {
        return 20;
    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreActingEvent pre) {
            ToolUseBlock tu = pre.getToolUse();
            if (tu != null && sensitiveTools.contains(tu.getName())) {
                log.warn("[Hook] intercept sensitive tool name={} id={} input_keys={} —— 将进入审批队列",
                        tu.getName(), tu.getId(), tu.getInput().keySet());
            }
            return Mono.just(event);
        }

        if (event instanceof PostActingEvent post) {
            ToolUseBlock tu = post.getToolUse();
            ToolResultBlock result = post.getToolResult();
            if (tu != null && sensitiveTools.contains(tu.getName())
                    && result != null && result.isSuspended()) {
                log.info("[Hook] tool={} id={} suspended -> 等待人工审批",
                        tu.getName(), tu.getId());
            }
            return Mono.just(event);
        }

        return Mono.just(event);
    }
}
