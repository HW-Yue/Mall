/**
 * Agent 测试对话抽屉 - 类型定义
 */

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  /** 流式输出时是否为未完成消息（持续追加） */
  streaming?: boolean;
  createdAt?: number;
}

export interface AgentOption {
  agentId: string;
  configId: string;
  configName: string;
}

export interface ChatDrawerProps {
  /** 是否显示抽屉 */
  visible: boolean;
  /** 关闭回调 */
  onClose: () => void;
  /** 打开时自动选中的 Agent ID（如从测试运行传入） */
  initialAgentId?: string;
  /** 可选：打开时使用的配置记录，用于显示名称 */
  initialConfig?: { configId: string; configName: string; agentId: string };
}
