window.RunWorkbench = (() => {
    const alertExamples = [
        { key: 'ServiceDown', group: 'system', severity: 'critical', application: 'mall-service' },
        { key: 'JvmHeapUsageHigh', group: 'system', severity: 'warning', application: 'order-service' },
        { key: 'JvmGcPauseHigh', group: 'system', severity: 'warning', application: 'order-service' },
        {
            key: 'Http5xxErrorRateHigh',
            group: 'http',
            severity: 'critical',
            application: 'mall-service',
            annotations: {
                summary: 'mall-service 接口 5xx 比例过高',
                description: '模拟商城对外入口异常率升高；application 以服务标签为准，网关前缀 /gw 不进入告警 labels.resource',
            },
        },
        {
            key: 'SentinelRtHigh',
            group: 'sentinel',
            severity: 'warning',
            application: 'order-service',
            labels: { resource: '/api/v1/order/get_pay_url' },
            annotations: {
                summary: 'order-service 获取支付 URL RT 过高',
                description: '模拟支付跳转链路变慢；resource 使用服务内 URI /api/v1/order/get_pay_url，不带 /gw',
            },
        },
        {
            key: 'SentinelBlockRateHigh',
            group: 'sentinel',
            severity: 'critical',
            application: 'mall-service',
            labels: { resource: '/api/v1/mall/index/query_goods_page' },
            annotations: {
                summary: 'mall 商品查询限流频繁',
                description: '模拟商城首页商品分页被 Sentinel 限流；resource 使用服务内 URI /api/v1/mall/index/query_goods_page',
            },
        },
        { key: 'HikariConnectionsSaturated', group: 'hikari', severity: 'critical', application: 'order-service' },
        { key: 'HikariConnectionsPending', group: 'hikari', severity: 'warning', application: 'order-service' },
        { key: 'HikariConnectionAcquireSlow', group: 'hikari', severity: 'warning', application: 'order-service' },
        { key: 'MySqlSlowQueriesHigh', group: 'mysql', severity: 'warning', application: 'order-service' },
        { key: 'MySqlTooManyConnections', group: 'mysql', severity: 'critical', application: 'order-service' },
        { key: 'MySqlDown', group: 'mysql', severity: 'critical', application: 'order-service' },
        { key: 'MySqlInnodbRowLockWaitHigh', group: 'mysql', severity: 'warning', application: 'order-service' },
        { key: 'RedisMemoryHigh', group: 'redis', severity: 'warning', application: 'group-buy-service' },
        { key: 'RedisDown', group: 'redis', severity: 'critical', application: 'group-buy-service' },
        { key: 'RedisBlockedClients', group: 'redis', severity: 'warning', application: 'group-buy-service' },
        { key: 'RedisKeyspaceHitRateLow', group: 'redis', severity: 'warning', application: 'group-buy-service' },
        { key: 'RedisConnectedClientsHigh', group: 'redis', severity: 'warning', application: 'group-buy-service' },
        {
            key: 'RocketMqConsumerLagHigh',
            group: 'rocketmq',
            severity: 'warning',
            application: 'order-service',
            labels: { topic: 'group-buy-order-create', consumerGroup: 'order-service-consumer' },
        },
        {
            key: 'RocketMqDlqMessageAppeared',
            group: 'rocketmq',
            severity: 'critical',
            application: 'order-service',
            labels: { topic: 'group-buy-order-create', consumerGroup: 'order-service-consumer' },
        },
        { key: 'RocketMqBrokerDown', group: 'rocketmq', severity: 'critical', application: 'shared' },
        { key: 'ThreadPoolQueueUsageHigh', group: 'dynamictp', severity: 'warning', application: 'order-service' },
        { key: 'ThreadPoolRejectedTasks', group: 'dynamictp', severity: 'critical', application: 'order-service' },
        { key: 'NacosConfigDrill', group: 'drill', severity: 'warning' },
    ];

    const examples = Object.fromEntries(alertExamples.map((item) => [
        item.key,
        {
            mode: 'alert',
            value: alertPayload(item),
        },
    ]));

    Object.assign(examples, {
        http5xx: examples.Http5xxErrorRateHigh,
        nacos: {
            ...examples.NacosConfigDrill,
            value: alertPayload({
                key: 'NacosConfigDrill',
                group: 'drill',
                severity: 'warning',
                application: 'yue-ops-agent',
                annotations: {
                    summary: 'Nacos 配置发布审批演练',
                    description: '触发含 nacos_publish_config 的 SOP，用于确认前端审批暂停与恢复',
                },
            }),
        },
        plain: {
            mode: 'text',
            value: '商城服务最近大量报错，用户反馈商品列表和下单链路偶发 500，请结合现有 SOP 给出排查草案。'
        }
    });

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
        dom.alertExample = document.getElementById('alert-example-select');
        dom.resultList = document.getElementById('run-result-list');
        dom.resultMeta = document.getElementById('run-result-meta');
        dom.resultEmpty = document.getElementById('run-result-empty');

        document.querySelectorAll('[data-route-mode]').forEach((button) => {
            button.addEventListener('click', () => {
                const mode = button.dataset.routeMode;
                if (state.mode === mode) return;
                if (mode === 'text') {
                    fillExample('plain');
                } else {
                    fillExample(dom.alertExample.value || 'http5xx');
                }
            });
        });
        document.querySelectorAll('[data-run-example]').forEach((button) => {
            button.addEventListener('click', () => fillExample(button.dataset.runExample));
        });
        dom.alertExample.addEventListener('change', () => fillExample(dom.alertExample.value));
        dom.form.addEventListener('submit', (event) => {
            event.preventDefault();
            submit();
        });
        dom.refresh.addEventListener('click', refresh);
        dom.cancel.addEventListener('click', cancel);

        renderAlertExamples();
        fillExample('http5xx');
        renderTimeline();
        renderRunResult();

        dom.resultList.addEventListener('click', (e) => {
            const btn = e.target.closest('.json-toggle-btn');
            if (btn) {
                const block = btn.closest('.json-block');
                block.classList.toggle('collapsed');
                btn.querySelector('.json-block-icon').textContent =
                    block.classList.contains('collapsed') ? '▶' : '▼';
                return;
            }
            const nodeToggle = e.target.closest('.json-node-toggle');
            if (nodeToggle) {
                const node = nodeToggle.closest('.json-node-wrap');
                node.classList.toggle('collapsed');
                nodeToggle.textContent = node.classList.contains('collapsed') ? '▶' : '▼';
            }
        });
    }

    function alertPayload(item) {
        const labels = {
            alertname: item.key,
            severity: item.severity,
            application: item.application || 'shared',
            instance: '192.168.1.10:18081',
            job: 'spring-boot',
            category: item.group,
            ...(item.labels || {}),
        };
        return {
            status: 'firing',
            alerts: [{
                status: 'firing',
                labels,
                annotations: {
                    summary: `测试告警 ${item.key}`,
                    description: `手动构造的 Alertmanager webhook 请求体，用于 /api/v1/alert/receive；业务资源请查看 labels.resource（服务内 URI，不带 /gw）`,
                    ...(item.annotations || {}),
                },
                startsAt: '2026-04-25T10:00:00Z',
                endsAt: '0001-01-01T00:00:00Z',
            }],
        };
    }

    function renderAlertExamples() {
        const groups = new Map();
        alertExamples.forEach((item) => {
            if (!groups.has(item.group)) {
                groups.set(item.group, []);
            }
            groups.get(item.group).push(item);
        });
        dom.alertExample.innerHTML = '';
        for (const [group, items] of groups.entries()) {
            const optgroup = document.createElement('optgroup');
            optgroup.label = group;
            items.forEach((item) => {
                const option = document.createElement('option');
                option.value = item.key;
                option.textContent = `${item.key} · ${item.severity}`;
                optgroup.appendChild(option);
            });
            dom.alertExample.appendChild(optgroup);
        }
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
        if (dom.alertExample && examples[dom.alertExample.value] !== item) {
            const alert = item.value?.alerts?.[0]?.labels?.alertname;
            if (alert && examples[alert]) {
                dom.alertExample.value = alert;
            }
        }
    }

    async function submit() {
        const value = dom.input.value.trim();
        if (!value) return;
        state.events = [];
        closeEventSource();
        renderTimeline();
        renderRunResult();
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
            renderRunResult();
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
            renderRunResult();
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
                renderRunResult();
            }
        } catch (error) {
            dom.status.textContent = `刷新失败: ${error.message}`;
        }
    }

    function renderTimeline() {
        if (!dom.timeline) return;
        if (!state.events.length) {
            dom.timeline.innerHTML = [
                '<div class="empty-list" role="status">',
                '<div class="empty-state-icon" aria-hidden="true">',
                '<svg width="40" height="40" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">',
                '<path d="M8 8h24v24H8z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" opacity="0.2"/>',
                '<path d="M12 16h8M12 20h16M12 24h10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" opacity="0.45"/>',
                '</svg></div>',
                '<p class="empty-state-title">时间线里还没有事件</p>',
                '<p class="empty-state-hint">提交运行后，会按顺序显示节点、匹配、工具、审批与结束等事件。</p>',
                '</div>',
            ].join('');
            return;
        }
        dom.timeline.innerHTML = [...state.events].reverse().map(renderEvent).join('');
    }

    function renderRunResult() {
        if (!dom.resultList || !dom.resultEmpty) return;
        if (!state.events.length) {
            dom.resultList.innerHTML = '';
            dom.resultList.hidden = true;
            dom.resultEmpty.hidden = false;
            if (dom.resultMeta) {
                dom.resultMeta.textContent = '提交运行后，这里展示最终输出与当前事件详情。';
            }
            return;
        }
        dom.resultEmpty.hidden = true;
        dom.resultList.hidden = false;
        dom.resultList.innerHTML = [...state.events].reverse().map(renderResultEvent).join('');
        if (dom.resultMeta) {
            const latest = state.events[state.events.length - 1];
            dom.resultMeta.textContent = `共 ${state.events.length} 个事件 · 最新: ${latest.type} · ${formatTime(latest.at)}`;
        }
    }

    function renderResultEvent(event) {
        const type = escapeHtml(event.type || 'event');
        const node = escapeHtml(event.node || '-');
        const message = escapeHtml(event.message || '');
        const at = escapeHtml(formatTime(event.at));
        const hasData = event.data && Object.keys(event.data).length > 0;
        return `
            <article class="timeline-item result-item ${escapeAttr(event.type || '')}">
                <div class="timeline-main">
                    <div>
                        <div class="timeline-title">${node}</div>
                        <div class="timeline-message">${message}</div>
                    </div>
                    <span class="status">${type}</span>
                </div>
                <div class="timeline-time">${at}</div>
                ${hasData ? renderJsonBlock(event.data) : ''}
            </article>
        `;
    }

    function renderEvent(event) {
        const type = escapeHtml(event.type || 'event');
        const node = escapeHtml(event.node || '-');
        const message = escapeHtml(event.message || '');
        const at = escapeHtml(formatTime(event.at));
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

    function renderJsonBlock(data) {
        const summary = jsonInlineSummary(data);
        return `
            <div class="json-block collapsed">
                <button type="button" class="json-toggle-btn">
                    <span class="json-block-icon">▶</span>
                    <span>查看数据</span>
                    <span class="json-block-hint">${escapeHtml(summary)}</span>
                </button>
                <div class="json-tree">${buildJsonTreeHtml(data, false)}</div>
            </div>`;
    }

    function buildJsonTreeHtml(value, initialCollapsed = true) {
        if (value === null) return `<span class="json-null">null</span>`;
        if (value === true) return `<span class="json-boolean">true</span>`;
        if (value === false) return `<span class="json-boolean">false</span>`;
        if (typeof value === 'number') return `<span class="json-number">${escapeHtml(String(value))}</span>`;
        if (typeof value === 'string') return `<span class="json-string">"${escapeHtml(value)}"</span>`;

        const isArr = Array.isArray(value);
        const entries = isArr
            ? value.map((v) => `<div class="json-row">${buildJsonTreeHtml(v)}</div>`)
            : Object.keys(value).map((k) =>
                `<div class="json-row"><span class="json-key">"${escapeHtml(k)}"</span>: ${buildJsonTreeHtml(value[k])}</div>`);

        if (!entries.length) return isArr ? `<span>[]</span>` : `<span>{}</span>`;

        const open = isArr ? '[' : '{';
        const close = isArr ? ']' : '}';
        const count = isArr ? `${entries.length} items` : `${entries.length} keys`;
        const collapsedClass = initialCollapsed ? 'collapsed' : '';
        const toggleIcon = initialCollapsed ? '▶' : '▼';

        return `<span class="json-node-wrap ${collapsedClass}">
            <span class="json-node-toggle" role="button">${toggleIcon}</span>
            <span class="json-inline-summary">${open}${escapeHtml(count)}${close}</span>
            <span class="json-expanded">
                <span>${open}</span>
                <div class="json-children">${entries.join('')}</div>
                <span>${close}</span>
            </span>
        </span>`;
    }

    function jsonInlineSummary(value) {
        if (value === null) return 'null';
        if (Array.isArray(value)) return `[…] ${value.length} items`;
        if (typeof value === 'object') return `{…} ${Object.keys(value).length} keys`;
        return String(value).slice(0, 40);
    }

    return { init, refresh };
})();
