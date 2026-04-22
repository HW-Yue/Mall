/*
 * ops-agent 审批收件箱模块
 *
 * 作为单页应用的一个 view。对外暴露 window.Inbox，包含：
 *   - init()              初始化 DOM 绑定 / 建 SSE 连接（幂等）
 *   - getPendingCount()   当前 PENDING 任务数
 *   - onPending(cb)       PENDING 数变化时回调（给 Tab 徽章用）
 *   - onConnection(cb)    SSE 连接状态变化（ online | offline ）
 *   - toast(msg, kind)    对外复用的轻量通知
 *
 * 聊天侧（chat.js）和 Tab 切换（app.js）使用这些接口，
 * 避免循环依赖，也方便后续把聊天换成 MCP + Skill 的 Agent。
 */
window.Inbox = (() => {
    const STATES = {
        tasks: new Map(),
        selectedId: null,
        filter: 'PENDING',
        domainFilter: '',
        initialized: false,
        pendingListeners: [],
        connListeners: [],
    };

    const els = {};

    function init() {
        if (STATES.initialized) return;
        STATES.initialized = true;

        els.list = document.getElementById('task-list');
        els.detail = document.getElementById('detail');
        els.toast = document.getElementById('toast');
        els.statusFilters = document.querySelectorAll('.filters button[data-status]');
        els.domainFilters = document.querySelectorAll('.filters button[data-domain]');

        els.statusFilters.forEach((btn) => {
            btn.addEventListener('click', () => {
                STATES.filter = btn.dataset.status;
                els.statusFilters.forEach((b) => b.classList.toggle('active', b === btn));
                renderList();
            });
        });
        els.domainFilters.forEach((btn) => {
            btn.addEventListener('click', () => {
                STATES.domainFilter = btn.dataset.domain;
                els.domainFilters.forEach((b) => b.classList.toggle('active', b === btn));
                renderList();
            });
        });

        renderList();
        renderDetail();
        connectSse();
    }

    // ================= 事件订阅 =================

    function onPending(cb) {
        STATES.pendingListeners.push(cb);
        cb(getPendingCount());
    }
    function onConnection(cb) {
        STATES.connListeners.push(cb);
    }
    function getPendingCount() {
        let n = 0;
        STATES.tasks.forEach((t) => {
            if (t.id !== '__heartbeat__' && t.status === 'PENDING') n++;
        });
        return n;
    }
    function firePending() {
        const n = getPendingCount();
        STATES.pendingListeners.forEach((cb) => { try { cb(n); } catch (_) {} });
    }
    function fireConnection(state) {
        STATES.connListeners.forEach((cb) => { try { cb(state); } catch (_) {} });
    }

    // ================= 渲染 =================

    function renderList() {
        if (!els.list) return;
        const rows = Array.from(STATES.tasks.values())
            .filter((t) => t.id !== '__heartbeat__')
            .filter((t) => !STATES.filter || t.status === STATES.filter)
            .filter((t) => !STATES.domainFilter || domainOf(t) === STATES.domainFilter)
            .sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''));

        if (rows.length === 0) {
            els.list.innerHTML = `<div class="task-empty">暂无匹配的任务</div>`;
            return;
        }

        els.list.innerHTML = rows
            .map((t) => {
                const domain = domainOf(t);
                return `
                    <div class="task-item ${STATES.selectedId === t.id ? 'selected' : ''}" data-id="${t.id}">
                        <div class="title">
                            <span class="data-id">${escapeHtml(t.dataId || '—')}</span>
                            <span class="badges">
                                <span class="badge domain ${escapeAttr(domain)}">${escapeHtml(domain)}</span>
                                <span class="badge ${t.status}">${t.status}</span>
                            </span>
                        </div>
                        <div class="meta">${escapeHtml(t.alertKey || '手动触发')}</div>
                        <div class="meta time">${formatTime(t.createdAt)}</div>
                    </div>
                `;
            })
            .join('');

        els.list.querySelectorAll('.task-item').forEach((el) => {
            el.addEventListener('click', () => selectTask(el.dataset.id));
        });
    }

    function domainOf(task) {
        const d = (task && task.domain) ? String(task.domain).toLowerCase() : '';
        return d || 'manual';
    }

    function selectTask(id) {
        STATES.selectedId = id;
        renderList();
        renderDetail();
    }

    function renderDetail() {
        if (!els.detail) return;
        const task = STATES.tasks.get(STATES.selectedId);
        if (!task) {
            els.detail.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">
                        <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M22 12h-6l-2 3h-4l-2-3H2"/>
                            <path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11Z"/>
                        </svg>
                    </div>
                    <div class="empty-title">请选择一个审批任务</div>
                    <div class="empty-desc">Prometheus 告警触发的 Nacos 变更方案会出现在左侧，待你拍板。</div>
                </div>`;
            return;
        }
        const alertBlock = task.alert
            ? `<pre class="code">${escapeHtml(JSON.stringify(task.alert, null, 2))}</pre>`
            : `<div class="muted">（无 alert 信息，可能是手动触发）</div>`;

        const reasoning = task.reasoning
            ? `<pre class="code">${escapeHtml(task.reasoning)}</pre>`
            : `<div class="muted">Agent 未提供 reasoning 字段</div>`;

        const canAct = task.status === 'PENDING';
        const domain = domainOf(task);
        const isNotify = domain === 'notify';
        const err = task.errorMessage
            ? `<div class="card danger"><h3>错误信息</h3><div>${escapeHtml(task.errorMessage)}</div></div>`
            : '';

        const diffView = isNotify
            ? { html: `<div class="muted">通知型任务不涉及 Nacos 配置变更。</div>`, summary: '—' }
            : renderDiff(task.contentBefore, task.contentAfter);

        const approveText = isNotify ? '知道了' : '同意并 Apply';
        const rejectText  = isNotify ? '忽略' : '拒绝';

        els.detail.innerHTML = `
            <div class="card">
                <h3>任务 <span class="accent">${task.id.slice(0, 8)}</span></h3>
                <div class="kv">
                    <b>域</b> <div class="kv-value"><span class="badge domain ${escapeAttr(domain)}">${escapeHtml(domain)}</span></div>
                    <b>状态</b> <div class="kv-value"><span class="badge ${task.status}">${task.status}</span></div>
                    <b>alertKey</b> <div class="kv-value">${escapeHtml(task.alertKey || '-')}</div>
                    <b>tool</b> <div class="kv-value">${escapeHtml(task.toolName || '-')}</div>
                    <b>dataId</b> <div class="kv-value">${escapeHtml(task.dataId || '-')}</div>
                    <b>group</b> <div class="kv-value">${escapeHtml(task.group || '-')}</div>
                    <b>创建时间</b> <div class="kv-value">${formatTime(task.createdAt)}</div>
                    <b>决策时间</b> <div class="kv-value">${task.decidedAt ? formatTime(task.decidedAt) : '-'}</div>
                </div>
            </div>

            ${err}

            <div class="card">
                <h3>触发告警</h3>
                ${alertBlock}
            </div>

            <div class="card">
                <h3>${isNotify ? '通知说明' : 'Agent 变更理由'}</h3>
                ${reasoning}
            </div>

            <div class="card">
                <h3>配置 Diff <span class="summary-inline">${diffView.summary}</span></h3>
                ${diffView.html}
            </div>

            <div class="card">
                <h3>决策</h3>
                <div class="actions">
                    <button class="btn approve" ${canAct ? '' : 'disabled'} id="btn-approve">${approveText}</button>
                    <button class="btn reject"  ${canAct ? '' : 'disabled'} id="btn-reject">${rejectText}</button>
                </div>
            </div>
        `;

        if (canAct) {
            document.getElementById('btn-approve').addEventListener('click', () => act('approve'));
            document.getElementById('btn-reject').addEventListener('click', () => act('reject'));
        }
    }

    // ================== IDEA-style side-by-side diff ==================

    function lineDiff(aLines, bLines) {
        const n = aLines.length;
        const m = bLines.length;
        const dp = [];
        for (let i = 0; i <= n; i++) dp.push(new Uint32Array(m + 1));
        for (let i = n - 1; i >= 0; i--) {
            for (let j = m - 1; j >= 0; j--) {
                if (aLines[i] === bLines[j]) dp[i][j] = dp[i + 1][j + 1] + 1;
                else dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
            }
        }
        const ops = [];
        let i = 0, j = 0;
        while (i < n && j < m) {
            if (aLines[i] === bLines[j]) {
                ops.push({ type: 'eq', left: aLines[i], right: bLines[j], li: i + 1, ri: j + 1 });
                i++; j++;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                ops.push({ type: 'del', left: aLines[i], li: i + 1 });
                i++;
            } else {
                ops.push({ type: 'ins', right: bLines[j], ri: j + 1 });
                j++;
            }
        }
        while (i < n) { ops.push({ type: 'del', left: aLines[i], li: i + 1 }); i++; }
        while (j < m) { ops.push({ type: 'ins', right: bLines[j], ri: j + 1 }); j++; }
        return ops;
    }

    function pairOps(ops) {
        const rows = [];
        let k = 0;
        while (k < ops.length) {
            if (ops[k].type === 'eq') {
                rows.push({
                    kind: 'eq',
                    lNo: ops[k].li, lText: ops[k].left,
                    rNo: ops[k].ri, rText: ops[k].right,
                });
                k++;
                continue;
            }
            const dels = [];
            while (k < ops.length && ops[k].type === 'del') { dels.push(ops[k]); k++; }
            const inss = [];
            while (k < ops.length && ops[k].type === 'ins') { inss.push(ops[k]); k++; }
            const len = Math.max(dels.length, inss.length);
            for (let t = 0; t < len; t++) {
                const d = dels[t];
                const a = inss[t];
                let kind;
                if (d && a) kind = 'chg';
                else if (d) kind = 'del';
                else kind = 'ins';
                rows.push({
                    kind,
                    lNo: d ? d.li : null, lText: d ? d.left : '',
                    rNo: a ? a.ri : null, rText: a ? a.right : '',
                });
            }
        }
        return rows;
    }

    function renderDiff(beforeRaw, afterRaw) {
        const before = prettyJson(beforeRaw);
        const after = prettyJson(afterRaw);
        const aLines = before.split('\n');
        const bLines = after.split('\n');
        const ops = lineDiff(aLines, bLines);
        const rows = pairOps(ops);

        let added = 0, removed = 0, changed = 0;
        for (const r of rows) {
            if (r.kind === 'ins') added++;
            else if (r.kind === 'del') removed++;
            else if (r.kind === 'chg') changed++;
        }

        const bodyHtml = rows.map((r) => {
            const lCls = r.kind === 'del' ? 'del' : r.kind === 'chg' ? 'del' : r.kind === 'ins' ? 'empty' : 'eq';
            const rCls = r.kind === 'ins' ? 'ins' : r.kind === 'chg' ? 'ins' : r.kind === 'del' ? 'empty' : 'eq';
            const lMark = (lCls === 'del') ? '-' : (lCls === 'empty' ? '' : ' ');
            const rMark = (rCls === 'ins') ? '+' : (rCls === 'empty' ? '' : ' ');
            return `
                <div class="diff-row">
                    <div class="diff-side ${lCls}">
                        <span class="gutter">${r.lNo ?? ''}</span>
                        <span class="marker">${lMark}</span>
                        <span class="line">${escapeHtml(r.lText)}</span>
                    </div>
                    <div class="diff-side ${rCls}">
                        <span class="gutter">${r.rNo ?? ''}</span>
                        <span class="marker">${rMark}</span>
                        <span class="line">${escapeHtml(r.rText)}</span>
                    </div>
                </div>
            `;
        }).join('');

        const html = `
            <div class="diff-ide">
                <div class="diff-header">
                    <div class="diff-side-title del-title">— BEFORE · 当前 Nacos</div>
                    <div class="diff-side-title ins-title">+ AFTER · Agent 拟写入</div>
                </div>
                <div class="diff-body">${bodyHtml}</div>
            </div>
        `;

        const summaryParts = [];
        if (changed > 0) summaryParts.push(`~${changed}`);
        if (added > 0)   summaryParts.push(`+${added}`);
        if (removed > 0) summaryParts.push(`-${removed}`);
        const summary = summaryParts.length ? summaryParts.join(' ') : '无变化';
        return { html, summary };
    }

    // ================== 基础工具 ==================

    async function act(kind) {
        const id = STATES.selectedId;
        if (!id) return;
        const url = `${window.API_BASE}/api/v1/approvals/${encodeURIComponent(id)}/${kind}`;
        try {
            const res = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: kind === 'reject' ? JSON.stringify({ reason: '前端手工拒绝' }) : '{}',
            });
            const body = await res.json();
            if (!res.ok) {
                toast(`${kind} 失败：${body.message || res.status}`, 'error');
                return;
            }
            upsertTask(body);
            toast(`${kind === 'approve' ? '已 Approve' : '已 Reject'}：${body.status}`, 'success');
        } catch (e) {
            toast(`网络错误：${e.message}`, 'error');
        }
    }

    function upsertTask(task) {
        if (!task || !task.id) return;
        if (task.id === '__heartbeat__') return;
        STATES.tasks.set(task.id, task);
        if (STATES.selectedId === task.id) {
            renderDetail();
        }
        renderList();
        firePending();
    }

    function prettyJson(raw) {
        if (raw == null) return '';
        try {
            return JSON.stringify(JSON.parse(raw), null, 2);
        } catch {
            return raw;
        }
    }

    function escapeHtml(s) {
        return String(s ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }
    function escapeAttr(s) {
        return String(s ?? '').replace(/[^a-zA-Z0-9_-]/g, '');
    }

    function formatTime(iso) {
        if (!iso) return '-';
        const d = new Date(iso);
        return Number.isNaN(d.getTime()) ? iso : d.toLocaleString();
    }

    function toast(msg, kind = 'success') {
        if (!els.toast) els.toast = document.getElementById('toast');
        if (!els.toast) return;
        els.toast.textContent = msg;
        els.toast.className = `toast ${kind} show`;
        clearTimeout(toast._timer);
        toast._timer = setTimeout(() => {
            els.toast.className = 'toast';
        }, 3500);
    }

    function connectSse() {
        const es = new EventSource(`${window.API_BASE}/api/v1/approvals/stream`);
        es.onopen = () => fireConnection('online');
        es.onerror = () => {
            fireConnection('offline');
            es.close();
            setTimeout(connectSse, 5000);
        };
        es.onmessage = (ev) => {
            try {
                upsertTask(JSON.parse(ev.data));
            } catch (e) {
                console.warn('bad SSE payload', ev.data, e);
            }
        };
    }

    return { init, onPending, onConnection, getPendingCount, toast };
})();
