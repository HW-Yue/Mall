(() => {
    const apiBase = window.API_BASE;
    const LAST_RUN_STORAGE_KEY = 'yue-ops-agent.last-run-id';
    const listEl = document.getElementById('runs-list');
    const countEl = document.getElementById('runs-count');
    const timelineEl = document.getElementById('timeline');
    const activeRunChip = document.getElementById('active-run-id');
    const timelineMeta = document.getElementById('timeline-meta');
    const timelineDesc = document.getElementById('timeline-desc');
    const refreshBtn = document.getElementById('runs-refresh');
    const runIdInput = document.getElementById('run-id-input');

    let activeRunId = null;
    let runMap = new Map();

    async function fetchRecent() {
        try {
            const resp = await fetch(`${apiBase}/ops/runs/recent?size=50`);
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            return await resp.json();
        } catch (e) {
            console.warn('recent 加载失败', e);
            showToast(`加载最近运行失败: ${e.message}`);
            return { runs: [], count: 0 };
        }
    }

    async function fetchHistory() {
        try {
            const resp = await fetch(`${apiBase}/ops/runs/history?size=50`);
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            return await resp.json();
        } catch (e) {
            console.warn('history 加载失败', e);
            showToast(`加载历史失败: ${e.message}`);
            return { runs: [], count: 0 };
        }
    }

    async function fetchTimeline(runId) {
        try {
            const resp = await fetch(`${apiBase}/ops/runs/${encodeURIComponent(runId)}/timeline`);
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            return await resp.json();
        } catch (e) {
            console.warn('timeline 加载失败', e);
            showToast(`加载 timeline 失败: ${e.message}`);
            return { runId, events: [], count: 0 };
        }
    }

    async function fetchRun(runId) {
        try {
            const resp = await fetch(`${apiBase}/ops/runs/${encodeURIComponent(runId)}`);
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            return await resp.json();
        } catch (e) {
            console.warn('run 加载失败', e);
            return { status: 'error', message: e.message, events: [] };
        }
    }

    function showToast(msg) {
        const t = document.getElementById('toast');
        if (!t) return;
        t.textContent = msg;
        t.classList.add('show');
        setTimeout(() => t.classList.remove('show'), 2400);
    }

    function renderRunCards(runs) {
        listEl.innerHTML = '';
        countEl.textContent = runs.length;
        if (!runs.length) {
            listEl.innerHTML = '<div class="empty" style="padding:24px;text-align:center;color:#9ca3af;">暂无最近运行。先在工作台发起一次运行；若要查看长期历史，请确认 ES / logstash 已启用。</div>';
            return;
        }
        runs.forEach((r) => {
            const card = document.createElement('div');
            card.className = 'run-card';
            card.dataset.runId = r.runId;
            const first = r.firstEvent || {};
            const last = r.lastEvent || {};
            const summary = first.eventMessage || first.node || '(无摘要)';
            const lastType = last.eventType || '-';
            const source = sourceLabel(r.source);
            const status = r.status || lastType;
            card.innerHTML = `
                <div class="run-id">${escapeHtml(r.runId)}</div>
                <div class="run-summary">${escapeHtml(summary)}</div>
                <div class="run-meta">
                    <span>${escapeHtml(source)} · ${escapeHtml(String(status))}</span>
                    <span>${escapeHtml(lastType)} · ${r.eventCount || 0} 事件</span>
                </div>
                <div class="run-meta">
                    <span>${escapeHtml(formatTime(r.latest || r.updatedAt || r.createdAt))}</span>
                    <span>${escapeHtml((r.inputType || '').toString())}</span>
                </div>
            `;
            card.addEventListener('click', () => selectRun(r.runId));
            if (r.runId === activeRunId) card.classList.add('active');
            listEl.appendChild(card);
        });
    }

    function renderTimeline(payload, options = {}) {
        const events = payload.events || [];
        timelineEl.innerHTML = '';
        timelineMeta.textContent = `${events.length} 个事件`;
        if (timelineDesc) {
            timelineDesc.textContent = options.desc || '事件按时间升序展示；data 字段渲染为可折叠 JSON。';
        }
        if (!events.length) {
            timelineEl.innerHTML = `<div class="empty">${escapeHtml(options.emptyMessage || '未查到事件。')}</div>`;
            return;
        }
        events.forEach((ev) => {
            const div = document.createElement('div');
            div.className = 'event';
            const type = ev.eventType || ev.type || 'event';
            const time = ev['@timestamp'] || ev.at;
            div.innerHTML = `
                <div class="event-head">
                    <span class="event-type ${escapeHtml(type)}">${escapeHtml(type)}</span>
                    <span class="event-time">${escapeHtml(formatTime(time))}</span>
                </div>
                <div class="event-node">${escapeHtml(ev.node || '')}</div>
                <div class="event-message">${escapeHtml(ev.eventMessage || ev.message || '')}</div>
            `;
            const tree = document.createElement('div');
            tree.className = 'json-tree';
            tree.appendChild(renderJson(ev.data, true));
            div.appendChild(tree);
            timelineEl.appendChild(div);
        });
    }

    function renderJson(value, root = false) {
        if (value === null || value === undefined) {
            return spanWith('null', 'null');
        }
        if (typeof value === 'string') return spanWith(`"${value}"`, 'string');
        if (typeof value === 'number') return spanWith(String(value), 'number');
        if (typeof value === 'boolean') return spanWith(String(value), 'boolean');
        if (Array.isArray(value)) {
            if (!value.length) return spanWith('[]');
            const details = document.createElement('details');
            if (root) details.open = true;
            const summary = document.createElement('summary');
            summary.textContent = `Array(${value.length})`;
            details.appendChild(summary);
            value.forEach((item, i) => {
                const row = document.createElement('div');
                const key = document.createElement('span');
                key.className = 'key';
                key.textContent = `${i}: `;
                row.appendChild(key);
                row.appendChild(renderJson(item));
                details.appendChild(row);
            });
            return details;
        }
        if (typeof value === 'object') {
            const keys = Object.keys(value);
            if (!keys.length) return spanWith('{}');
            const details = document.createElement('details');
            if (root) details.open = true;
            const summary = document.createElement('summary');
            summary.textContent = `Object(${keys.length})`;
            details.appendChild(summary);
            keys.forEach((k) => {
                const row = document.createElement('div');
                const key = document.createElement('span');
                key.className = 'key';
                key.textContent = `${k}: `;
                row.appendChild(key);
                row.appendChild(renderJson(value[k]));
                details.appendChild(row);
            });
            return details;
        }
        return spanWith(String(value));
    }

    function spanWith(text, cls) {
        const s = document.createElement('span');
        if (cls) s.className = cls;
        s.textContent = text;
        return s;
    }

    function escapeHtml(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function formatTime(ts) {
        if (!ts) return '-';
        try {
            const d = new Date(ts);
            if (isNaN(d.getTime())) return ts;
            return d.toLocaleString('zh-CN', { hour12: false });
        } catch (e) {
            return ts;
        }
    }

    async function selectRun(runId) {
        activeRunId = runId;
        localStorage.setItem(LAST_RUN_STORAGE_KEY, runId);
        activeRunChip.textContent = runId;
        document.querySelectorAll('.run-card').forEach((el) => {
            el.classList.toggle('active', el.dataset.runId === runId);
        });
        timelineEl.innerHTML = '<div class="empty">加载中...</div>';
        const run = runMap.get(runId);
        if (run?.source === 'memory') {
            const snapshot = await fetchRun(runId);
            if (Array.isArray(snapshot.events) && snapshot.events.length) {
                renderTimeline(
                    { runId, events: snapshot.events, count: snapshot.events.length },
                    { desc: '当前进程内实时事件列表，优先于 ES 历史。' }
                );
                return;
            }
        }
        const data = await fetchTimeline(runId);
        if ((data.events || []).length) {
            renderTimeline(data, { desc: '来自 Elasticsearch 的归档事件时间线。' });
            return;
        }
        const snapshot = await fetchRun(runId);
        if (Array.isArray(snapshot.events) && snapshot.events.length) {
            renderTimeline(
                { runId, events: snapshot.events, count: snapshot.events.length },
                { desc: 'ES 中暂无该 run，已回退到当前进程内实时事件。' }
            );
            return;
        }
        renderTimeline(
            { runId, events: [], count: 0 },
            { emptyMessage: '未查到该 run 的事件。若是历史运行，请确认 ES / logstash 已启用；若是刚创建的运行，请回到工作台确认当前进程仍在。' }
        );
    }

    async function reload() {
        const [recent, history] = await Promise.all([fetchRecent(), fetchHistory()]);
        const runs = mergeRuns(recent.runs || [], history.runs || []);
        runMap = new Map(runs.map((item) => [item.runId, item]));
        renderRunCards(runs);
        const preferredRunId = resolvePreferredRunId(runs);
        if (preferredRunId) {
            runIdInput.value = preferredRunId;
            await selectRun(preferredRunId);
            return;
        }
        activeRunId = null;
        activeRunChip.textContent = '未选中';
        renderTimeline({ events: [] }, { emptyMessage: '当前没有可查看的运行。' });
    }

    function resolvePreferredRunId(runs) {
        const runIdFromUrl = new URLSearchParams(window.location.search).get('runId');
        if (runIdFromUrl) return runIdFromUrl;
        const lastRunId = localStorage.getItem(LAST_RUN_STORAGE_KEY);
        if (lastRunId && runs.some((item) => item.runId === lastRunId)) return lastRunId;
        return runs[0]?.runId || '';
    }

    function mergeRuns(recentRuns, historyRuns) {
        const merged = new Map();
        recentRuns.forEach((item) => {
            merged.set(item.runId, normalizeRun(item, 'memory'));
        });
        historyRuns.forEach((item) => {
            if (!merged.has(item.runId)) {
                merged.set(item.runId, normalizeRun(item, 'es'));
            }
        });
        return [...merged.values()].sort((a, b) => {
            const left = new Date(a.latest || a.updatedAt || a.createdAt || 0).getTime();
            const right = new Date(b.latest || b.updatedAt || b.createdAt || 0).getTime();
            return right - left;
        });
    }

    function normalizeRun(run, defaultSource) {
        return {
            ...run,
            source: run.source || defaultSource,
            latest: run.latest || run.updatedAt || run.createdAt || run.lastEvent?.['@timestamp'] || run.lastEvent?.at || '',
        };
    }

    function sourceLabel(source) {
        if (source === 'memory') return '当前运行';
        if (source === 'db') return 'DB 摘要';
        return '历史归档';
    }

    refreshBtn.addEventListener('click', reload);
    runIdInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            const v = runIdInput.value.trim();
            if (v) selectRun(v);
        }
    });

    reload();
})();
