package com.yue.opsagent.adapter.persistence.approval;

import com.yue.opsagent.domain.model.AlertEvent;
import com.yue.opsagent.domain.model.ApprovalStatus;
import com.yue.opsagent.domain.model.ApprovalTask;

import java.time.OffsetDateTime;

/**
 * 审批任务的持久化对象（和 MyBatis 绑定）。
 *
 * <p>与 {@link ApprovalTask} record 解耦：domain 层保持不可变 record，基础设施层自行维护
 * 带 setter 的 POJO，方便 MyBatis 反射填充。</p>
 *
 * <p>字段命名采用驼峰，对应数据库下划线列（由 {@code map-underscore-to-camel-case} 自动映射）。
 * 唯一的特殊字段是 {@code groupName} ↔ {@code group_name}（避开 MySQL 保留字 group）。</p>
 */
public class ApprovalTaskPO {

    private String id;
    private String alertKey;
    /** JSON 列，通过自定义 TypeHandler 与 {@link AlertEvent} 双向序列化。 */
    private AlertEvent alert;
    private String domain;
    private String toolName;
    private String dataId;
    /** 数据库列名为 {@code group_name}（group 是 MySQL 保留字）。 */
    private String groupName;
    private String contentBefore;
    private String contentAfter;
    private String reasoning;
    private String status;
    private String errorMessage;
    private OffsetDateTime createdAt;
    private OffsetDateTime decidedAt;

    public ApprovalTaskPO() {
    }

    public static ApprovalTaskPO from(ApprovalTask task) {
        if (task == null) {
            return null;
        }
        ApprovalTaskPO po = new ApprovalTaskPO();
        po.id = task.id();
        po.alertKey = task.alertKey();
        po.alert = task.alert();
        po.domain = task.domain();
        po.toolName = task.toolName();
        po.dataId = task.dataId();
        po.groupName = task.group();
        po.contentBefore = task.contentBefore();
        po.contentAfter = task.contentAfter();
        po.reasoning = task.reasoning();
        po.status = task.status() == null ? null : task.status().name();
        po.errorMessage = task.errorMessage();
        po.createdAt = task.createdAt();
        po.decidedAt = task.decidedAt();
        return po;
    }

    public ApprovalTask toDomain() {
        return new ApprovalTask(
                id,
                alertKey,
                alert,
                domain,
                toolName,
                dataId,
                groupName,
                contentBefore,
                contentAfter,
                reasoning,
                status == null ? null : ApprovalStatus.valueOf(status),
                errorMessage,
                createdAt,
                decidedAt
        );
    }

    // ------------------ getters / setters ------------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAlertKey() { return alertKey; }
    public void setAlertKey(String alertKey) { this.alertKey = alertKey; }

    public AlertEvent getAlert() { return alert; }
    public void setAlert(AlertEvent alert) { this.alert = alert; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public String getDataId() { return dataId; }
    public void setDataId(String dataId) { this.dataId = dataId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getContentBefore() { return contentBefore; }
    public void setContentBefore(String contentBefore) { this.contentBefore = contentBefore; }

    public String getContentAfter() { return contentAfter; }
    public void setContentAfter(String contentAfter) { this.contentAfter = contentAfter; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(OffsetDateTime decidedAt) { this.decidedAt = decidedAt; }
}
