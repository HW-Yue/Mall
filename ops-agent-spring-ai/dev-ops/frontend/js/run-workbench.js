window.RunWorkbench = (() => {
    const examples = {
        http5xx: {
            mode: 'alert',
            value: {
                status: 'firing',
                alerts: [{
                    status: 'firing',
                    labels: {
                        alertname: 'Http5xxErrorRateHigh',
                        severity: 'critical',
                        application: 'order-service',
                        category: 'http',
                        instance: 'order-service:8080'
                    },
                    annotations: {
                        summary: 'order-service 5xx 错误率过高',
                        description: '最近 5 分钟 5xx 错误率超过阈值'
                    }
                }]
            }
        },
        nacos: {
            mode: 'alert',
            value: {
                status: 'firing',
                alerts: [{
                    status: 'firing',
                    labels: {
                        alertname: 'NacosConfigDrill',
                        severity: 'warning',
                        application: 'ops-agent-spring-ai',
                        category: 'nacos'
                    },
                    annotations: {
                        summary: 'Nacos 配置发布审批演练',
                        description: '触发含 nacos_publish_config 的 SOP，用于确认前端审批暂停与恢复'
                    }
                }]
            }
        },
        plain: {
            mode: 'text',
            value: '订单服务最近大量报错，用户反馈下单接口偶发 500，请选择已有 SOP 或给出排查草案。'
        }
    };

    const state = {
        mode: 'alert',
        runId: '',
        eventSource: null,
        events: []
    };
    const dom = {};

    function init() {
        dom.form = document.getElementById('route-form');
        dom.input = document.getElementById('route-input');
        dom.submit = document.getElementById('run-submit');
        dom.cancel = document.getElementById('run-cancel');
        dom.runId = document.getElementById('run-id');
        dom.status = document.getElementById('run-status');
        dom.timeline = document.getElementById('timeline');
        dom.refresh = document.getElementById('run-refresh');

        document.querySelectorAll('[data-route-mode]').forEach((button) => {
            button.addEventListener('click', () => setMode(button.dataset.routeMode));
        });
        document.querySelectorAll('[data-run-example]').forEach((button) => {
            button.addEventListener('click', () => fillExample(button.dataset.runExample));
        });
        dom.form.addEventListener('submit', (event) => {
            event.preventDefault();
            submit();
        });
        dom.refresh.addEventListener('click', refresh);
        dom.cancel.addEventListener('click', cancel);

        fillExample('http5xx');
        renderTimeline();
    }

    function setMode(mode) {
        state.mode = mode;
        document.querySelectorAll('[data-route-mode]').forEach((button) => {
            button.classList.toggle('active', button.dataset.routeMode === mode);
        });
        dom.input.placeholder = mode === 'alert' ? '粘贴 Alertmanager JSON' : '输入纯文本运维描述';
    }

    function fillExample(key) {
        const item = examples[key];
        if (!item) return;
        setMode(item.mode);
        dom.input.value = item.mode === 'alert' ? JSON.stringify(item.value, null, 2) : item.value;
    }

    async function submit() {
        const value = dom.input.value.trim();
        if (!value) return;
        state.events = [];
        closeEventSource();
        renderTimeline();
        setBusy(true);
        dom.status.textContent = '提交中...';
        try {
            const res = state.mode === 'alert'
                ? await fetch(`${window.API_BASE}/alert/receive`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: value
                })
                : await fetch(`${window.API_BASE}/ops/route-text`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'text/plain;charset=UTF-8' },
                    body: value
                });
            const text = await res.text();
            const body = tryParseJson(text);
            if (!res.ok) throw new Error(`HTTP ${res.status}: ${text.slice(0, 600)}`);
            const runId = extractRunId(body);
            if (!runId) throw new Error('后端未返回 runId');
            state.runId = runId;
            dom.runId.textContent = runId;
            dom.refresh.disabled = false;
            dom.cancel.disabled = false;
            connect(runId);
            await refresh();
            window.Approvals?.sync?.();
        } catch (error) {
            dom.status.textContent = `提交失败: ${error.message}`;
            state.events.push({
                type: 'failed',
                node: 'Frontend',
                message: error.message,
                at: new Date().toISOString(),
                data: {}
            });
            renderTimeline();
        } finally {
            setBusy(false);
        }
    }

    function extractRunId(body) {
        if (!body) return '';
        if (body.runId) return body.runId;
        if (Array.isArray(body.runs) && body.runs[0]) return body.runs[0].runId;
        return '';
    }

    function connect(runId) {
        closeEventSource();
        const es = new EventSource(`${window.API_BASE}/ops/runs/${encodeURIComponent(runId)}/events`);
        state.eventSource = es;
        es.onmessage = (event) => {
            const item = tryParseJson(event.data);
            if (!item || item.type === 'heartbeat') return;
            state.events.push(item);
            renderTimeline();
            if (isTerminalEvent(item.type)) {
                dom.cancel.disabled = true;
                refresh();
            }
        };
        es.onerror = () => {
            dom.status.textContent = '事件流断开，仍可手动刷新状态。';
            closeEventSource();
        };
    }

    async function refresh() {
        if (!state.runId) return;
        try {
            const res = await fetch(`${window.API_BASE}/ops/runs/${encodeURIComponent(state.runId)}`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const body = await res.json();
            dom.status.textContent = `${body.status || '-'} · ${body.currentNode || '-'}`;
            dom.cancel.disabled = !state.runId || isTerminalStatus(body.status);
            if (Array.isArray(body.events) && body.events.length > state.events.length) {
                state.events = body.events;
                renderTimeline();
            }
        } catch (error) {
            dom.status.textContent = `刷新失败: ${error.message}`;
        }
    }

    function renderTimeline() {
        if (!dom.timeline) return;
        if (!state.events.length) {
            dom.timeline.innerHTML = '<div class="empty-list">暂无事件。提交后会显示节点、匹配、工具、审批和结束事件。</div>';
            return;
        }
        dom.timeline.innerHTML = state.events.map(renderEvent).join('');
        dom.timeline.scrollTop = dom.timeline.scrollHeight;
    }

    function renderEvent(event) {
        const type = escapeHtml(event.type || 'event');
        const node = escapeHtml(event.node || '-');
        const message = escapeHtml(event.message || '');
        const at = escapeHtml(formatTime(event.at));
        const data = event.data && Object.keys(event.data).length
            ? `<pre>${escapeHtml(JSON.stringify(event.data, null, 2))}</pre>`
            : '';
        return `
            <article class="timeline-item ${escapeAttr(event.type || '')}">
                <div class="timeline-main">
                    <div>
                        <div class="timeline-title">${node}</div>
                        <div class="timeline-message">${message}</div>
                    </div>
                    <span class="status">${type}</span>
                </div>
                <div class="timeline-time">${at}</div>
                ${data}
            </article>
        `;
    }

    function setBusy(busy) {
        dom.submit.disabled = busy;
    }

    async function cancel() {
        if (!state.runId || dom.cancel.disabled) return;
        dom.cancel.disabled = true;
        try {
            const res = await fetch(`${window.API_BASE}/ops/runs/${encodeURIComponent(state.runId)}/cancel`, {
                method: 'POST'
            });
            const body = await res.json();
            if (!res.ok) throw new Error(body.message || `HTTP ${res.status}`);
            dom.status.textContent = `${body.status || 'CANCELLED'} · 已发送暂停`;
            await refresh();
        } catch (error) {
            dom.status.textContent = `暂停失败: ${error.message}`;
            dom.cancel.disabled = false;
        }
    }

    function isTerminalEvent(type) {
        return type === 'end'
            || type === 'failed'
            || type === 'approval_rejected'
            || type === 'cancelled';
    }

    function isTerminalStatus(status) {
        return status === 'COMPLETED'
            || status === 'FAILED'
            || status === 'REJECTED'
            || status === 'CANCELLED';
    }

    function closeEventSource() {
        if (state.eventSource) {
            state.eventSource.close();
            state.eventSource = null;
        }
    }

    function tryParseJson(text) {
        try {
            return JSON.parse(text);
        } catch {
            return null;
        }
    }

    function formatTime(value) {
        if (!value) return '';
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? value : date.toLocaleTimeString();
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    function escapeAttr(value) {
        return String(value ?? '').replace(/[^a-zA-Z0-9_-]/g, '');
    }

    return { init, refresh };
})();
