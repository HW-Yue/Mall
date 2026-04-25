package com.yue.opsagent.springai.infrastructure.observability;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 日志中输出大段文本时的统一截断，避免撑爆 ELK/文件。
 */
public final class OpsLogFormatter {

    /** 单条日志里「发往/来自模型」等内容默认上限 */
    public static final int LLM_BODY_MAX = 16_384;

    /** Webhook / 告警 JSON 默认上限 */
    public static final int ALERT_JSON_MAX = 48_000;

    /** 子 Agent 多轮消息里，单条 role 块上限 */
    public static final int SUB_MSG_PART_MAX = 8_192;

    private OpsLogFormatter() {
    }

    public static String truncate(String s, int maxChars) {
        if (s == null) {
            return "";
        }
        if (s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, maxChars) + "\n... [已截断，总长度 " + s.length() + " 字符]";
    }

    /**
     * 将当前多轮对话压缩为一段可读文本，便于日志对齐「和大模型的交互」。
     */
    public static String summarizeMessages(List<Message> messages, int perPartMax, int totalMax) {
        if (messages == null || messages.isEmpty()) {
            return "(无消息)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            String role = m.getMessageType() == null ? "?" : m.getMessageType().name();
            sb.append('[').append(i).append(']').append(role).append('\n');
            sb.append(truncate(m.getText(), perPartMax)).append("\n---\n");
        }
        return truncate(sb.toString(), totalMax);
    }
}
