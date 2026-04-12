import React from 'react';
import styled from 'styled-components';
import { IconComment } from '@douyinfe/semi-icons';
import { theme } from '../../styles/theme';
import { useChatDrawer } from '../../contexts/chat-drawer-context';

const TriggerBall = styled.button`
  position: fixed;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  border-radius: 12px 0 0 12px;
  border: none;
  border-right: none;
  background: rgba(24, 144, 255, 0.25);
  color: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  z-index: ${theme.zIndex.modal - 1};
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.08);
  transition: background ${theme.animation.duration.fast} ${theme.animation.easing.ease},
    color ${theme.animation.duration.fast} ${theme.animation.easing.ease},
    box-shadow ${theme.animation.duration.fast} ${theme.animation.easing.ease};

  &:hover {
    background: rgba(24, 144, 255, 0.5);
    color: #fff;
    box-shadow: -2px 0 12px rgba(24, 144, 255, 0.25);
  }

  &:active {
    background: rgba(24, 144, 255, 0.6);
  }

  .semi-icons {
    font-size: 22px;
  }
`;

export const ChatDrawerTrigger: React.FC = () => {
  const { visible, openDrawer } = useChatDrawer();

  if (visible) return null;

  return (
    <TriggerBall
      type="button"
      onClick={() => openDrawer()}
      title="打开 Agent 测试对话"
      aria-label="打开 Agent 测试对话"
    >
      <IconComment />
    </TriggerBall>
  );
};
