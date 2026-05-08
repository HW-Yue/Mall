(() => {
    const apiBase = window.API_BASE;
    const listEl = document.getElementById('runs-list');
    const countEl = document.getElementById('runs-count');
    const timelineEl = document.getElementById('timeline');
    const activeRunChip = document.getElementById('active-run-id');
    const timelineMeta = document.getElementById('timeline-meta');
    const refreshBtn = document.getElementById('runs-refresh');
    const runIdInput = document.getElementById('run-id-input');

    let activeRunId = null;

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
            listEl.innerHTML = '<div class="empty" style="padding:24px;text-align:center;color:#9ca3af;">暂无历史，或 ES 索引无数据</div>';
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
            card.innerHTML = `
                <div class="run-id">${escapeHtml(r.runId)}</div>
                <div class="run-summary">${escapeHtml(summary)}</div>
                <div class="run-meta">
                    <span>${escapeHtml(lastType)} · ${r.eventCount || 0} 事件</span>
                    <span>${escapeHtml(formatTime(r.latest))}</span>
                </div>
            `;
            card.addEventListener('click', () => selectRun(r.runId));
            if (r.runId === activeRunId) card.classList.add('active');
            listEl.appendChild(card);
        });
    }

    function renderTimeline(payload) {
        const events = payload.events || [];
        timelineEl.innerHTML = '';
        timelineMeta.textContent = `${events.length} 个事件`;
        if (!events.length) {
            timelineEl.innerHTML = '<div class="empty">未查到事件，确认 logstash.enabled=true 且 ES 中有该 runId 数据</div>';
            return;
        }
        events.forEach((ev) => {
            const div = document.createElement('div');
            div.className = 'event';
            const type = ev.eventType || 'event';
            div.innerHTML = `
                <div class="event-head">
                    <span class="event-type ${escapeHtml(type)}">${escapeHtml(type)}</span>
                    <span class="event-time">${escapeHtml(formatTime(ev['@timestamp']))}</span>
                </div>
                <div class="event-node">${escapeHtml(ev.node || '')}</div>
                <div class="event-message">${escapeHtml(ev.eventMessage || '')}</div>
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
        activeRunChip.textContent = runId;
        document.querySelectorAll('.run-card').forEach((el) => {
            el.classList.toggle('active', el.dataset.runId === runId);
        });
        timelineEl.innerHTML = '<div class="empty">加载中...</div>';
        const data = await fetchTimeline(runId);
        renderTimeline(data);
    }

    async function reload() {
        const data = await fetchHistory();
        renderRunCards(data.runs || []);
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
