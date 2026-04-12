import { useClientContext } from '@flowgram.ai/free-layout-editor';
import { WorkflowNodeType } from '../nodes/constants';

/**
 * 从当前画布中获取 Agent 节点选中的策略（strategy）。
 * 用于 Client 节点二级联动：未选策略时禁用/引导，选策略后拉取 client_type 槽位与实例。
 */
export function useAgentStrategy(): string {
  const clientContext = useClientContext();
  try {
    const json = clientContext.document.toJSON() as { nodes?: Array<{ type?: string; data?: { inputsValues?: { strategy?: string } } }> };
    const nodes = json?.nodes ?? [];
    const agent = nodes.find((n) => n?.type === WorkflowNodeType.Agent);
    const strategy = agent?.data?.inputsValues?.strategy;
    return typeof strategy === 'string' ? strategy : '';
  } catch {
    return '';
  }
}
