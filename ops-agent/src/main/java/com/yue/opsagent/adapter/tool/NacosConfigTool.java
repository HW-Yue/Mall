package com.yue.opsagent.adapter.tool;

import com.yue.opsagent.adapter.hook.AlertContextHolder;
import com.yue.opsagent.domain.model.AlertEvent;
import com.yue.opsagent.domain.model.ApprovalStatus;
import com.yue.opsagent.domain.model.ApprovalTask;
import com.yue.opsagent.domain.port.ApprovalInboxPort;
import com.yue.opsagent.domain.port.NacosConfigPort;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.ToolSuspendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 出站适配器：面向 AgentScope 的 Tool 层。
 *
 * <p><b>publishConfig 走审批流</b>：Agent 调用后不会立刻写 Nacos，而是：
 * <ol>
 *     <li>拉当前原值 {@code contentBefore}</li>
 *     <li>把 {@link ApprovalTask} 写入 {@link ApprovalInboxPort}（状态 PENDING，SSE 广播前端）</li>
 *     <li>抛 {@link ToolSuspendException}，AgentScope 框架自动把本轮 run 以
 *         {@code GenerateReason.TOOL_SUSPENDED} 收尾，会话干净挂起</li>
 * </ol>
 * 真正的 {@code configService.publishConfig} 由 {@code ApprovalService#apply} 在人工
 * Approve 后直接走 {@link NacosConfigPort} 完成。</p>
 */
@Component
public class NacosConfigTool {

    private static final Logger log = LoggerFactory.getLogger(NacosConfigTool.class);

    private final NacosConfigPort configPort;
    private final ApprovalInboxPort inbox;

    public NacosConfigTool(NacosConfigPort configPort, ApprovalInboxPort inbox) {
        this.configPort = configPort;
        this.inbox = inbox;
    }

    @Tool(description = "从 Nacos 读取配置原文。可用于读取 Sentinel 规则、DynamicTP 配置等，不涉及写入。")
    public String getConfig(
            @ToolParam(name = "dataId", description = "配置的 dataId") String dataId,
            @ToolParam(name = "group", description = "配置分组，例如 SENTINEL_GROUP / DEFAULT_GROUP") String group) {
        return configPort.getConfig(dataId, group);
    }

    /**
     * 发布配置到 Nacos（敏感操作）。<b>不会立刻生效</b>：会转为一条待人工审批的任务，
     * Agent 立即挂起；审批通过后由后端异步 apply。
     */
    @Tool(description = """
            提交一次 Nacos 配置变更提案（敏感操作）。
            注意：本调用不会立刻写入 Nacos，而是进入人工审批队列；Agent 随后会挂起等待。
            请务必：
              - content 字段提供完整的新配置原文（不要只写 diff）
              - reasoning 字段说明为什么这样改、参考指标、before/after 对比
            审批被拒或超时，当前方案作废；无需重试。
            """)
    public String publishConfig(
            @ToolParam(name = "dataId", description = "配置的 dataId，例如 mall-flow-rules.json") String dataId,
            @ToolParam(name = "group", description = "配置分组，Sentinel 规则用 SENTINEL_GROUP") String group,
            @ToolParam(name = "content", description = "完整新配置原文（保留未变动的条目）") String content,
            @ToolParam(name = "reasoning",
                    description = "变更理由 + 参考指标 + before/after 对比。",
                    required = false) String reasoning,
            @ToolParam(name = "source_alert_key",
                    description = "触发本次变更的告警键 alertname|application|resource，手动调用可留空。",
                    required = false) String sourceAlertKey) {

        AlertEvent alert = AlertContextHolder.get();
        String domain = AlertContextHolder.getDomain();
        if (domain == null || domain.isBlank()) {
            domain = "manual";
        }
        String resolvedAlertKey = sourceAlertKey;
        if (resolvedAlertKey == null && alert != null) {
            resolvedAlertKey = alert.alertname() + "|" + alert.application() + "|" + alert.resource();
        }

        String before;
        try {
            before = configPort.getConfig(dataId, group);
        } catch (RuntimeException ex) {
            log.warn("[Tool] publishConfig 拉取 before 失败 dataId={} group={}: {}",
                    dataId, group, ex.getMessage());
            before = null;
        }

        String taskId = UUID.randomUUID().toString();
        ApprovalTask task = new ApprovalTask(
                taskId,
                resolvedAlertKey,
                alert,
                domain,
                "publishConfig",
                dataId,
                group,
                before,
                content,
                reasoning,
                ApprovalStatus.PENDING,
                null,
                OffsetDateTime.now(),
                null
        );
        inbox.save(task);

        log.info("[Tool] publishConfig -> 审批队列 taskId={} domain={} dataId={} group={} alertKey={}",
                taskId, domain, dataId, group, resolvedAlertKey);

        throw new ToolSuspendException(
                "已提交审批 id=" + taskId
                        + "，dataId=" + dataId + " group=" + group
                        + "。请在 devops/index.html#inbox 审批通过后配置才会真正写入 Nacos。");
    }

    @Tool(description = "删除 Nacos 配置。注意：删除也是敏感操作，当前版本不走审批，请谨慎使用。")
    public String deleteConfig(
            @ToolParam(name = "dataId", description = "配置的 dataId") String dataId,
            @ToolParam(name = "group", description = "配置分组") String group) {
        boolean ok = configPort.deleteConfig(dataId, group);
        return ok ? "删除成功" : "删除失败";
    }
}
