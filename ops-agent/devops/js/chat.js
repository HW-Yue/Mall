/*
 * ops-agent 对话模块
 *
 * 目前对接的后端是 AG-UI 协议（/agui/run）→ NacosManagerAgent。
 * 后续你会把它替换成带 MCP + Skill 的 Agent —— 届时只需：
 *   1. 换掉 AguiClient 的 endpoint（或换一个 client 实现）
 *   2. 在 Chat.config 里覆盖 endpoint / payload builder
 *   3. UI 层 DOM 结构保持不变
 */
window.Chat = (() => {
    const STATE = {
        initialized: false,
        threadId: 'thread-' + Date.now(),
        history: [],
        running: false,
        client: null,
    };

    const dom = {};

    const CONFIG = {
        // 后续要切 MCP Agent 时只需改这里（或整体换成另一个 client）
        endpoint: () => `${window.API_BASE}/agui/run`,
    };

    function init() {
        if (STATE.initialized) return;
        STATE.initialized = true;

        dom.messages = document.getElementById('messages');
        dom.input = document.getElementById('input');
        dom.sendBtn = document.getElementById('send-btn');
        dom.stopBtn = document.getElementById('stop-btn');
        dom.composer = document.getElementById('composer');
        dom.clearBtn = document.getElementById('chat-clear');

        STATE.client = new AguiClient(CONFIG.endpoint());

        dom.composer.addEventListener('submit', (e) => {
            e.preventDefault();
            send();
        });
        dom.stopBtn.addEventListener('click', stop);
        dom.clearBtn.addEventListener('click', resetThread);

        dom.input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
                e.preventDefault();
                send();
            }
        });
        dom.input.addEventListener('input', autoResize);

        // 快捷 chip
        document.querySelectorAll('.chip[data-prompt]').forEach((chip) => {
            chip.addEventListener('click', () => {
                dom.input.value = chip.dataset.prompt;
                autoResize();
                dom.input.focus();
            });
        });
    }

    function focus() { dom.input?.focus(); }

    // ================= 核心发送 =================

    async function send() {
        const text = dom.input.value.trim();
        if (!text || STATE.running) return;

        dom.input.value = '';
        autoResize();

        STATE.running = true;
        dom.sendBtn.hidden = true;
        dom.stopBtn.hidden = false;

        append('user', text);
        STATE.history.push({ id: 'msg-' + Date.now(), role: 'user', content: text });

        const typing = showTyping();

        let assistantContent = '';
        let reasoningContent = '';
        let currentMessageId = null;
        let currentAssistantEl = null;
        let currentReasoningEl = null;

        try {
            await STATE.client.run({
                threadId: STATE.threadId,
                runId: 'run-' + Date.now(),
                messages: STATE.history,
            }, {
                onRunStarted: () => {
                    currentAssistantEl = null;
                    currentReasoningEl = null;
                    reasoningContent = '';
                },
                onReasoningMessageStart: () => {
                    hideTyping(typing);
                    currentReasoningEl = null;
                    reasoningContent = '';
                },
                onReasoningContent: (delta) => {
                    if (!currentReasoningEl) {
                        currentReasoningEl = append('reasoning', delta);
                    } else {
                        appendContent(currentReasoningEl, delta);
                    }
                    reasoningContent += delta;
                },
                onReasoningMessageEnd: () => {
                    currentReasoningEl = null;
                    reasoningContent = '';
                },
                onTextMessageStart: (messageId) => {
                    hideTyping(typing);
                    currentMessageId = messageId;
                    assistantContent = '';
                    currentAssistantEl = null;
                },
                onTextContent: (delta) => {
                    if (!currentAssistantEl) {
                        currentAssistantEl = append('assistant', delta);
                    } else {
                        appendContent(currentAssistantEl, delta);
                    }
                    assistantContent += delta;
                },
                onTextMessageEnd: (messageId) => {
                    if (assistantContent) {
                        STATE.history.push({
                            id: messageId,
                            role: 'assistant',
                            content: assistantContent,
                        });
                    }
                    currentAssistantEl = null;
                },
                onToolCallStart: (_id, name) => {
                    hideTyping(typing);
                    currentAssistantEl = null;
                    append('tool', `调用工具：${name}`);
                },
                onError: (error) => {
                    hideTyping(typing);
                    append('error', `错误：${error}`);
                },
                onRunFinished: () => {
                    hideTyping(typing);
                },
            });
        } catch (error) {
            hideTyping(typing);
            if (error.name === 'AbortError') {
                if (assistantContent) {
                    STATE.history.push({
                        id: currentMessageId || 'msg-stopped-' + Date.now(),
                        role: 'assistant',
                        content: assistantContent + ' [stopped]',
                    });
                }
            } else {
                append('error', `错误：${error.message}`);
            }
        } finally {
            STATE.running = false;
            dom.sendBtn.hidden = false;
            dom.stopBtn.hidden = true;
            hideTyping(typing);
        }
    }

    function stop() {
        if (STATE.running) STATE.client.abort();
    }

    function resetThread() {
        STATE.threadId = 'thread-' + Date.now();
        STATE.history = [];
        dom.messages.innerHTML = '';
        dom.input.focus();
        window.Inbox?.toast?.('已开启新对话', 'info');
    }

    // ================= DOM ops =================

    function append(role, text) {
        const el = document.createElement('div');
        el.className = `msg ${role}`;
        const roleLabel = {
            user: '你',
            assistant: 'Agent',
            tool: '工具',
            reasoning: '思考',
            error: '错误',
        }[role] || role;
        el.innerHTML = `<span class="msg-role">${roleLabel}</span><span class="msg-body">${escapeHtml(text)}</span>`;
        dom.messages.appendChild(el);
        scrollToBottom();
        return el;
    }

    function appendContent(el, delta) {
        const body = el.querySelector('.msg-body');
        if (body) body.textContent += delta;
        scrollToBottom();
    }

    function showTyping() {
        const el = document.createElement('div');
        el.className = 'typing';
        el.innerHTML = '<span></span><span></span><span></span>';
        dom.messages.appendChild(el);
        scrollToBottom();
        return el;
    }
    function hideTyping(el) { if (el && el.parentNode) el.remove(); }

    function scrollToBottom() {
        dom.messages.scrollTop = dom.messages.scrollHeight;
    }

    function autoResize() {
        dom.input.style.height = 'auto';
        dom.input.style.height = Math.min(dom.input.scrollHeight, 180) + 'px';
    }

    function escapeHtml(s) {
        return String(s ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    return { init, focus, resetThread, config: CONFIG };
})();
