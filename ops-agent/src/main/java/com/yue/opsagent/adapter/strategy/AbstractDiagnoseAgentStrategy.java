package com.yue.opsagent.adapter.strategy;

import com.yue.opsagent.agent.DiagnoseAgent;
import com.yue.opsagent.config.AgentSystemPromptProperties;
import com.yue.opsagent.config.ApprovalProperties;
import com.yue.opsagent.domain.model.AlertEvent;
import com.yue.opsagent.domain.model.ApprovalStatus;
import com.yue.opsagent.domain.model.ApprovalTask;
import com.yue.opsagent.domain.port.ApprovalInboxPort;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * "诊断子 Agent"类策略基类。在 {@link AbstractNotifyOnlyStrategy} 之上叠加一个异步诊断步骤：
 * Redis / RocketMQ 等 NotifyOnly 域告警进来后，不再仅仅把 mapping 反查表贴在 reasoning 里，
 * 而是立刻把 PENDING 任务落盘（reasoning 占位为"诊断中..."），同时把告警 JSON 喂给
 * {@link DiagnoseAgent}（通过 MCP 调 Elasticsearch 查日志），跑完再异步把结论回填到
 * {@link ApprovalInboxPort#updateReasoning(String, String)}。
 *
 * <h3>时序</h3>
 * <pre>
 * dispatch() ──┬─▶ inbox.save(PENDING, reasoning="[诊断中]...")
 *              │
 *              └─▶ DiagnoseAgent.createAgent(prompt).call(userMsg)
 *                        ├─ onSuccess → inbox.updateReasoning(taskId, text)
 *                        └─ onError   → inbox.updateReasoning(taskId, "[诊断失败] "+err)
 * </pre>
 *
 * <h3>子类需要实现</h3>
 * <ul>
 *   <li>{@link #domain()} / {@link #order()}（沿用 {@link AlertStrategy} 接口）</li>
 *   <li>{@link #matches(AlertEvent)} —— 控制 category 过滤</li>
 *   <li>{@link #buildDiagnosePrompt(AlertEvent, String)} —— 产出 user prompt，
 *       基类负责把 prompt 喂进 DiagnoseAgent；system prompt 从 {@code ops-agent.prompts.<domain>-diagnose}
 *       里取（例如 {@code redis-diagnose}）</li>
 * </ul>
 *
 * <p>当 {@link DiagnoseAgent#isReady()} 返回 false（MCP 未启用或握手失败）时，整条诊断
 * 通路降级：任务仍然落 inbox，但 reasoning 直接写 {@code "[诊断失败] ES MCP 未就绪"}，
 * 不影响主链路。</p>
 */
public abstract class AbstractDiagnoseAgentStrategy extends AbstractNotifyOnlyStrategy {

    protected final AgentSystemPromptProperties promptProps;
    protected final DiagnoseAgent diagnoseAgent;

    protected AbstractDiagnoseAgentStrategy(ApprovalProperties approvalProps,
                                            ApprovalInboxPort inbox,
                                            AgentSystemPromptProperties promptProps,
                                            DiagnoseAgent diagnoseAgent) {
        super(approvalProps, inbox);
        this.promptProps = promptProps;
        this.diagnoseAgent = diagnoseAgent;
    }

    /**
     * 诊断 system prompt 的 YAML 键，例如 {@code redis-diagnose} 对应
     * {@code ops-agent.prompts.redis-diagnose}。默认是 {@code domain()+"-diagnose"}，
     * 子类可覆盖自定义。
     */
    protected String diagnosePromptKey() {
        return domain() + "-diagnose";
    }

    /**
     * 子类构造喂给 DiagnoseAgent 的用户 prompt。通常至少包含：
     * <ul>
     *   <li>告警 JSON（便于 Agent 抽取 alertname / labels / time）</li>
     *   <li>responsible service 提示（pool-name / client-name 反查）</li>
     *   <li>建议的索引命名 {@code nexus-<app>-YYYY.MM.dd}</li>
     * </ul>
     */
    protected abstract String buildDiagnosePrompt(AlertEvent event, String alertKey);

    @Override
    public void dispatch(AlertEvent event, String alertKey) {
        String taskId = UUID.randomUUID().toString();
        ApprovalTask task = new ApprovalTask(
                taskId,
                alertKey,
                event,
                domain(),
                "",
                "-",
                "-",
                null,
                null,
                "[诊断中...] 已提交 ES MCP 子 Agent 异步排查，稍后回填结论。",
                ApprovalStatus.PENDING,
                null,
                OffsetDateTime.now(),
                null
        );
        inbox.save(task);
        log.info("[{}] 已投递诊断型任务 alertKey={} taskId={} alertname={}",
                domain(), alertKey, taskId, event.alertname());

        if (!diagnoseAgent.isReady()) {
            String msg = "[诊断失败] ES MCP 未就绪，请检查 ops-agent.mcp.elasticsearch.enabled / url 与 elasticsearch-mcp 容器状态。";
            inbox.updateReasoning(taskId, msg);
            log.warn("[{}] DiagnoseAgent 未就绪，跳过诊断 taskId={}", domain(), taskId);
            return;
        }

        String sysPrompt = promptProps.getPrompts()
                .getOrDefault(diagnosePromptKey(), promptProps.getSystemPrompt());
        String promptBody = buildDiagnosePrompt(event, alertKey);

        ReActAgent agent = diagnoseAgent.createAgent(sysPrompt, domain() + "-diagnose");
        if (agent == null) {
            inbox.updateReasoning(taskId,
                    "[诊断失败] DiagnoseAgent 构建失败，请查看 ops-agent 日志。");
            return;
        }

        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .textContent(promptBody)
                .build();

        log.info("[{}] diagnose agent run started alertKey={} taskId={} promptBytes={}",
                domain(), alertKey, taskId, promptBody.length());

        agent.call(userMsg)
                .subscribe(
                        result -> {
                            String text = result == null ? null : result.getTextContent();
                            if (text == null || text.isBlank()) {
                                inbox.updateReasoning(taskId, "[诊断失败] 子 Agent 未返回文本结论。");
                            } else {
                                inbox.updateReasoning(taskId, text);
                                log.info("[{}] diagnose agent finished alertKey={} taskId={} bytes={}",
                                        domain(), alertKey, taskId, text.length());
                            }
                        },
                        err -> {
                            log.error("[{}] diagnose agent failed alertKey={} taskId={}: {}",
                                    domain(), alertKey, taskId, err.toString(), err);
                            inbox.updateReasoning(taskId,
                                    "[诊断失败] " + err.getClass().getSimpleName() + ": " + err.getMessage());
                        });
    }
}
