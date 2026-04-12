import React, { createContext, useContext } from 'react';

/**
 * 在代理配置页提供自定义 Run 行为（与代理列表「测试执行」一致）。
 * 当 onRunClick 存在时，画布底部 Run 按钮将触发测试运行弹窗，而非画布仿真。
 */
export interface AgentConfigRunContextValue {
  onRunClick?: () => void;
}

const AgentConfigRunContext = createContext<AgentConfigRunContextValue>({});

export const AgentConfigRunProvider: React.FC<{
  value: AgentConfigRunContextValue;
  children: React.ReactNode;
}> = ({ value, children }) => (
  <AgentConfigRunContext.Provider value={value}>
    {children}
  </AgentConfigRunContext.Provider>
);

export function useAgentConfigRun(): AgentConfigRunContextValue {
  return useContext(AgentConfigRunContext);
}
