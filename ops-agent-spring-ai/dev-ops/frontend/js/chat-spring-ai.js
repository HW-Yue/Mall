window.Chat = (() => {
    const state = {
        initialized: false,
        running: false,
        mode: 'stream',
        abortController: null,
    };
    const dom = {};

    function init() {
        if (state.initialized) return;
        state.initialized = true;

        dom.messages = document.getElementById('messages');
        dom.input = document.getElementById('input');
        dom.form = document.getElementById('composer');
        dom.send = document.getElementById('send-btn');
        dom.stop = document.getElementById('stop-btn');
        dom.clear = document.getElementById('chat-clear');

        dom.form.addEventListener('submit', (event) => {
            event.preventDefault();
            send();
        });
        dom.stop.addEventListener('click', stop);
        dom.clear.addEventListener('click', clear);
        dom.input.addEventListener('input', resizeInput);
        dom.input.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
                event.preventDefault();
                send();
            }
        });

        document.querySelectorAll('[data-chat-mode]').forEach((button) => {
            button.addEventListener('click', () => {
                state.mode = button.dataset.chatMode;
                document.querySelectorAll('[data-chat-mode]').forEach((item) => {
                    item.classList.toggle('active', item === button);
                });
                dom.input.focus();
            });
        });

        document.querySelectorAll('[data-prompt]').forEach((button) => {
            button.addEventListener('click', () => {
                dom.input.value = button.dataset.prompt;
                resizeInput();
                dom.input.focus();
            });
        });
    }

    async function send() {
        const message = dom.input.value.trim();
        if (!message || state.running) return;

        state.running = true;
        state.abortController = new AbortController();
        dom.input.value = '';
        resizeInput();
        setRunning(true);
        appendMessage('user', message);

        const pending = appendMessage('assistant pending', '思考中');
        let answer = '';

        try {
            if (state.mode === 'react') {
                const res = await fetch(`${window.API_BASE}/chat/react`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ message }),
                    signal: state.abortController.signal,
                });
                const bodyText = await res.text();
                if (!res.ok) throw new Error(`HTTP ${res.status}: ${bodyText.slice(0, 600)}`);
                const body = JSON.parse(bodyText);
                answer = body.reply || JSON.stringify(body, null, 2);
                replaceMessage(pending, 'assistant', answer);
                return;
            }

            const res = await fetch(`${window.API_BASE}/chat/stream`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Accept: 'text/event-stream',
                },
                body: JSON.stringify({ message }),
                signal: state.abortController.signal,
            });
            if (!res.ok) throw new Error(`HTTP ${res.status}: ${(await res.text()).slice(0, 600)}`);

            replaceMessage(pending, 'assistant', '');
            answer = await readSse(res, (chunk) => {
                answer += chunk;
                setBody(pending, answer);
            });
        } catch (error) {
            if (error.name === 'AbortError') {
                replaceMessage(pending, 'assistant', answer || '已停止。');
            } else {
                replaceMessage(pending, 'error', error.message);
            }
        } finally {
            state.running = false;
            state.abortController = null;
            setRunning(false);
            dom.input.focus();
        }
    }

    async function readSse(response, onChunk) {
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        let full = '';

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });
            let boundary;
            while ((boundary = buffer.indexOf('\n\n')) !== -1) {
                const block = buffer.slice(0, boundary);
                buffer = buffer.slice(boundary + 2);
                full += consumeBlock(block, onChunk);
            }
        }
        if (buffer.trim()) full += consumeBlock(buffer, onChunk);
        return full;
    }

    function consumeBlock(block, onChunk) {
        let collected = '';
        for (const line of block.split(/\r?\n/)) {
            if (!line.startsWith('data:')) continue;
            const chunk = line.startsWith('data: ') ? line.slice(6) : line.slice(5);
            if (chunk === '[DONE]') continue;
            collected += chunk;
            onChunk(chunk);
        }
        return collected;
    }

    function appendMessage(kind, text) {
        const el = document.createElement('article');
        el.className = `message ${kind}`;
        el.innerHTML = `<div class="message-role">${roleName(kind)}</div><div class="message-body"></div>`;
        setBody(el, text);
        dom.messages.appendChild(el);
        scroll();
        return el;
    }

    function replaceMessage(el, kind, text) {
        el.className = `message ${kind}`;
        el.querySelector('.message-role').textContent = roleName(kind);
        setBody(el, text);
    }

    function setBody(el, text) {
        el.querySelector('.message-body').textContent = text;
        scroll();
    }

    function roleName(kind) {
        if (kind.includes('user')) return '你';
        if (kind.includes('error')) return '错误';
        return state.mode === 'react' ? 'ReAct Agent' : 'Chat';
    }

    function stop() {
        state.abortController?.abort();
    }

    function clear() {
        dom.messages.innerHTML = '';
    }

    function setRunning(running) {
        dom.send.disabled = running;
        dom.stop.hidden = !running;
    }

    function resizeInput() {
        dom.input.style.height = 'auto';
        dom.input.style.height = Math.min(dom.input.scrollHeight, 160) + 'px';
    }

    function scroll() {
        dom.messages.scrollTop = dom.messages.scrollHeight;
    }

    return { init };
})();
