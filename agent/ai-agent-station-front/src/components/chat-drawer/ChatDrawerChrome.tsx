import React from 'react';
import { ChatDrawerTrigger } from './ChatDrawerTrigger';
import { ChatDrawer } from './ChatDrawer';
import { useChatDrawer } from '../../contexts/chat-drawer-context';

/**
 * 全局抽屉 + 右侧悬浮入口，需在 ChatDrawerProvider 内使用
 */
export const ChatDrawerChrome: React.FC = () => {
  const { visible, closeDrawer, initialAgentId, initialConfig } = useChatDrawer();

  return (
    <>
      <ChatDrawerTrigger />
      <ChatDrawer
        visible={visible}
        onClose={closeDrawer}
        initialAgentId={initialAgentId}
        initialConfig={initialConfig}
      />
    </>
  );
};
