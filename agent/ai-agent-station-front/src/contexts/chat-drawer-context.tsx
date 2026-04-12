import React, { createContext, useContext, useState, useCallback } from 'react';

export interface ChatDrawerInitialConfig {
  configId: string;
  configName: string;
  agentId: string;
}

interface ChatDrawerContextValue {
  visible: boolean;
  initialAgentId?: string;
  initialConfig?: ChatDrawerInitialConfig;
  openDrawer: (payload?: { initialAgentId?: string; initialConfig?: ChatDrawerInitialConfig }) => void;
  closeDrawer: () => void;
}

const ChatDrawerContext = createContext<ChatDrawerContextValue | null>(null);

export function ChatDrawerProvider({ children }: { children: React.ReactNode }) {
  const [visible, setVisible] = useState(false);
  const [initialAgentId, setInitialAgentId] = useState<string | undefined>();
  const [initialConfig, setInitialConfig] = useState<ChatDrawerInitialConfig | undefined>();

  const openDrawer = useCallback(
    (payload?: { initialAgentId?: string; initialConfig?: ChatDrawerInitialConfig }) => {
      setInitialAgentId(payload?.initialAgentId);
      setInitialConfig(payload?.initialConfig);
      setVisible(true);
    },
    []
  );

  const closeDrawer = useCallback(() => {
    setVisible(false);
  }, []);

  const value: ChatDrawerContextValue = {
    visible,
    initialAgentId,
    initialConfig,
    openDrawer,
    closeDrawer,
  };

  return (
    <ChatDrawerContext.Provider value={value}>
      {children}
    </ChatDrawerContext.Provider>
  );
}

export function useChatDrawer(): ChatDrawerContextValue {
  const ctx = useContext(ChatDrawerContext);
  if (!ctx) {
    throw new Error('useChatDrawer must be used within ChatDrawerProvider');
  }
  return ctx;
}
