import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Button, Select, Toast } from '@douyinfe/semi-ui';
import { IconDelete, IconSend } from '@douyinfe/semi-icons';
import styled from 'styled-components';
import { theme } from '../../styles/theme';
import { AiAgentDrawService } from '../../services/ai-agent-draw-service';
import { API_ENDPOINTS } from '../../config';
import type { ChatDrawerProps, ChatMessage, AgentOption } from './types';
import { MarkdownContent } from './MarkdownContent';

const DRAWER_WIDTH_MIN = 400;
const DRAWER_WIDTH_DEFAULT = 520;

function getDrawerWidthMax(): number {
  if (typeof window === 'undefined') return 900;
  return Math.floor(window.innerWidth * 0.8);
}
const SESSION_STORAGE_KEY = 'agent_chat_session_id';
const MAX_STEP = 5;

const Overlay = styled.div<{ $visible: boolean }>`
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: ${theme.zIndex.modal};
  opacity: ${props => props.$visible ? 1 : 0};
  visibility: ${props => props.$visible ? 'visible' : 'hidden'};
  transition: opacity ${theme.animation.duration.normal} ${theme.animation.easing.ease},
    visibility ${theme.animation.duration.normal} ${theme.animation.easing.ease};
`;

const DrawerPanel = styled.div<{ $width: number; $visible: boolean }>`
  position: fixed;
  top: 0;
  right: 0;
  width: ${props => props.$width}px;
  height: 100vh;
  background: ${theme.colors.bg.primary};
  box-shadow: -4px 0 16px rgba(0, 0, 0, 0.12);
  z-index: ${theme.zIndex.modal + 1};
  transform: translateX(${props => props.$visible ? '0' : '100%'});
  transition: transform ${theme.animation.duration.normal} ${theme.animation.easing.cubic};
  display: flex;
  flex-direction: column;
`;

const ResizeHandle = styled.div`
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 6px;
  cursor: col-resize;
  z-index: 1;
  &:hover {
    background: rgba(24, 144, 255, 0.1);
  }
`;

const DrawerHeader = styled.div`
  flex-shrink: 0;
  padding: ${theme.spacing.base} ${theme.spacing.lg};
  border-bottom: 1px solid ${theme.colors.border.secondary};
  display: flex;
  align-items: center;
  gap: ${theme.spacing.sm};
`;

const AgentSelectWrap = styled.div`
  flex: 1;
  min-width: 0;
`;

const DrawerBody = styled.div`
  flex: 1;
  overflow-y: auto;
  padding: ${theme.spacing.base};
  display: flex;
  flex-direction: column;
  gap: ${theme.spacing.base};
`;

const MessageBubble = styled.div<{ $role: 'user' | 'assistant' }>`
  max-width: 85%;
  align-self: ${props => props.$role === 'user' ? 'flex-end' : 'flex-start'};
  padding: ${theme.spacing.sm} ${theme.spacing.base};
  border-radius: ${theme.borderRadius.lg};
  font-size: ${theme.typography.fontSize.sm};
  line-height: ${theme.typography.lineHeight.relaxed};
  ${props =>
    props.$role === 'user'
      ? `
    background: ${theme.colors.primary};
    color: #fff;
  `
      : `
    background: ${theme.colors.bg.tertiary};
    color: ${theme.colors.text.primary};
    border: 1px solid ${theme.colors.border.secondary};
  `}
`;

const DrawerFooter = styled.div`
  flex-shrink: 0;
  padding: ${theme.spacing.base} ${theme.spacing.lg};
  border-top: 1px solid ${theme.colors.border.secondary};
  background: ${theme.colors.bg.primary};
`;

const TextAreaWrap = styled.div`
  display: flex;
  gap: ${theme.spacing.sm};
  align-items: flex-end;
  .semi-textarea-wrapper {
    flex: 1;
  }
  textarea {
    min-height: 72px;
    max-height: 160px;
    resize: none;
  }
`;

export const ChatDrawer: React.FC<ChatDrawerProps> = ({
  visible,
  onClose,
  initialAgentId,
  initialConfig,
}) => {
  const [width, setWidth] = useState(() =>
    Math.min(getDrawerWidthMax(), Math.max(DRAWER_WIDTH_MIN, DRAWER_WIDTH_DEFAULT))
  );
  const [agentList, setAgentList] = useState<AgentOption[]>([]);
  const [selectedAgent, setSelectedAgent] = useState<AgentOption | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [sessionId, setSessionId] = useState<string>(() => {
    try {
      return sessionStorage.getItem(SESSION_STORAGE_KEY) || '';
    } catch {
      return '';
    }
  });
  const bodyRef = useRef<HTMLDivElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    const el = bodyRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [messages]);

  useEffect(() => {
    const onResize = () => {
      const maxW = getDrawerWidthMax();
      setWidth((prev) => Math.min(maxW, Math.max(DRAWER_WIDTH_MIN, prev)));
    };
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);

  const isDraggingRef = useRef(false);
  const startXRef = useRef(0);
  const startWidthRef = useRef(0);

  // 持久化 sessionId
  useEffect(() => {
    if (sessionId) {
      try {
        sessionStorage.setItem(SESSION_STORAGE_KEY, sessionId);
      } catch {}
    }
  }, [sessionId]);

  // 加载 Agent 列表（绘图配置列表，含 agentId）
  useEffect(() => {
    if (!visible) return;
    AiAgentDrawService.queryDrawConfigList({ pageNum: 1, pageSize: 100 })
      .then((list) => {
        const options: AgentOption[] = list.map((item) => ({
          agentId: item.agentId,
          configId: item.configId,
          configName: item.configName || item.configId,
        }));
        setAgentList(options);
      })
      .catch(() => Toast.error('加载 Agent 列表失败'));
  }, [visible]);

  // 打开时根据 initialAgentId / initialConfig 选中对应 Agent，否则默认选中第一个
  useEffect(() => {
    if (!visible || !agentList.length) return;
    if (initialConfig) {
      const byConfig = agentList.find((a) => a.configId === initialConfig.configId);
      if (byConfig) {
        setSelectedAgent(byConfig);
        return;
      }
    }
    if (initialAgentId) {
      const byAgent = agentList.find((a) => a.agentId === initialAgentId);
      if (byAgent) {
        setSelectedAgent(byAgent);
        return;
      }
    }
    setSelectedAgent(agentList[0]);
  }, [visible, initialAgentId, initialConfig, agentList]);

  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    isDraggingRef.current = true;
    startXRef.current = e.clientX;
    startWidthRef.current = width;
    document.body.style.userSelect = 'none';
    document.body.style.cursor = 'col-resize';
  }, [width]);

  useEffect(() => {
    const onMove = (e: MouseEvent) => {
      if (!isDraggingRef.current) return;
      const delta = startXRef.current - e.clientX;
      const maxW = getDrawerWidthMax();
      const next = Math.min(maxW, Math.max(DRAWER_WIDTH_MIN, startWidthRef.current + delta));
      setWidth(next);
    };
    const onUp = () => {
      if (!isDraggingRef.current) return;
      isDraggingRef.current = false;
      document.body.style.userSelect = '';
      document.body.style.cursor = '';
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    return () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
    };
  }, []);

  const clearChat = useCallback(() => {
    setMessages([]);
    setSessionId('');
    try {
      sessionStorage.removeItem(SESSION_STORAGE_KEY);
    } catch {}
    if (abortRef.current) {
      abortRef.current.abort();
      abortRef.current = null;
    }
    setLoading(false);
  }, []);

  const sendMessage = useCallback(async () => {
    const text = inputValue.trim();
    if (!text || !selectedAgent) {
      if (!selectedAgent) Toast.warning('请先选择 Agent');
      else Toast.warning('请输入内容');
      return;
    }
    if (loading) return;

    const userMsg: ChatMessage = {
      id: `user_${Date.now()}`,
      role: 'user',
      content: text,
      createdAt: Date.now(),
    };
    setMessages((prev) => [...prev, userMsg]);
    setInputValue('');
    const currentSessionId =
      sessionId || `session_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;
    if (!sessionId) setSessionId(currentSessionId);

    const assistantId = `assistant_${Date.now()}`;
    setMessages((prev) => [
      ...prev,
      { id: assistantId, role: 'assistant', content: '', streaming: true, createdAt: Date.now() },
    ]);
    setLoading(true);
    abortRef.current = new AbortController();

    const url = `${API_ENDPOINTS.AI_AGENT.BASE}${API_ENDPOINTS.AI_AGENT.AUTO_AGENT}`;
    const body = {
      aiAgentId: selectedAgent.agentId,
      message: text,
      sessionId: currentSessionId,
      maxStep: MAX_STEP,
    };

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
        body: JSON.stringify(body),
        signal: abortRef.current.signal,
      });
      if (!response.ok) {
        setMessages((prev) =>
          prev.map((m) =>
            m.id === assistantId
              ? { ...m, content: `请求失败: ${response.status}`, streaming: false }
              : m
          )
        );
        return;
      }
      const reader = response.body?.getReader();
      if (!reader) {
        setMessages((prev) =>
          prev.map((m) =>
            m.id === assistantId ? { ...m, content: '无法读取响应流', streaming: false } : m
          )
        );
        return;
      }
      const decoder = new TextDecoder();
      let fullContent = '';
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        const chunk = decoder.decode(value, { stream: true });
        const lines = chunk.split('\n');
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6).trim();
            if (data && data !== '[DONE]') {
              try {
                const json = JSON.parse(data);
                const content = json.content;
                if (content) {
                  fullContent += content;
                  setMessages((prev) =>
                    prev.map((m) =>
                      m.id === assistantId ? { ...m, content: fullContent } : m
                    )
                  );
                }
              } catch {
                fullContent += data;
                setMessages((prev) =>
                  prev.map((m) => (m.id === assistantId ? { ...m, content: fullContent } : m))
                );
              }
            }
          }
        }
      }
      setMessages((prev) =>
        prev.map((m) => (m.id === assistantId ? { ...m, content: fullContent, streaming: false } : m))
      );
    } catch (e) {
      if ((e as Error).name === 'AbortError') return;
      const msg = e instanceof Error ? e.message : String(e);
      setMessages((prev) =>
        prev.map((m) =>
          m.id === assistantId ? { ...m, content: `错误: ${msg}`, streaming: false } : m
        )
      );
      Toast.error('发送失败');
    } finally {
      setLoading(false);
      abortRef.current = null;
    }
  }, [inputValue, selectedAgent, loading, sessionId]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault();
      sendMessage();
    }
  };

  const handleOverlayClick = () => onClose();

  const agentOptions = agentList.map((a) => ({
    value: a.configId,
    label: `${a.configName} (${a.agentId})`,
  }));

  return (
    <>
      <Overlay $visible={visible} onClick={handleOverlayClick} aria-hidden />
      <DrawerPanel ref={panelRef} $width={width} $visible={visible}>
        <ResizeHandle onMouseDown={handleMouseDown} title="拖拽调整宽度" />
        <DrawerHeader>
          <AgentSelectWrap>
            <Select
              placeholder="选择 Agent"
              value={selectedAgent?.configId}
              optionList={agentOptions}
              onChange={(v) => {
                const nextValue = String(v);
                const opt = agentList.find((a) => a.configId === nextValue);
                setSelectedAgent(opt || null);
              }}
              getPopupContainer={() => panelRef.current || document.body}
              dropdownStyle={{ zIndex: theme.zIndex.modal + 3 }}
              style={{ width: '100%' }}
              size="default"
            />
          </AgentSelectWrap>
          <Button
            icon={<IconDelete />}
            type="tertiary"
            size="small"
            onClick={clearChat}
            title="清空对话"
          >
            清空
          </Button>
        </DrawerHeader>
        <DrawerBody ref={bodyRef}>
          {messages.length === 0 && (
            <div style={{ color: theme.colors.text.tertiary, fontSize: theme.typography.fontSize.sm, textAlign: 'center', padding: theme.spacing.xl }}>
              选择 Agent 后输入内容，按 Ctrl+Enter 发送
            </div>
          )}
          {messages.map((m) => (
            <MessageBubble key={m.id} $role={m.role}>
              {m.role === 'user' ? (
                m.content
              ) : (
                <MarkdownContent content={m.content || (m.streaming ? '...' : '')} />
              )}
            </MessageBubble>
          ))}
        </DrawerBody>
        <DrawerFooter>
          <TextAreaWrap>
            <textarea
              className="semi-input semi-input-default"
              placeholder="输入消息，Ctrl+Enter 发送"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={loading}
              rows={3}
              style={{
                width: '100%',
                padding: '8px 12px',
                borderRadius: theme.borderRadius.base,
                border: `1px solid ${theme.colors.border.primary}`,
                fontFamily: 'inherit',
                fontSize: theme.typography.fontSize.sm,
              }}
            />
            <Button
              theme="solid"
              type="primary"
              icon={<IconSend />}
              loading={loading}
              onClick={sendMessage}
            >
              发送
            </Button>
          </TextAreaWrap>
        </DrawerFooter>
      </DrawerPanel>
    </>
  );
};
