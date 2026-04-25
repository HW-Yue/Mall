window.Approvals = (() => {
    const state = {
        connection: 'offline',
        listeners: [],
        tasks: [],
    };
    const dom = {};

    function init() {
        dom.panel = document.querySelector('.approvals-panel');
        dom.list = document.getElementById('approval-list');
        dom.count = document.getElementById('approval-count');
        dom.refresh = document.getElementById('approval-refresh');
        dom.toast = document.getElementById('toast');

        dom.refresh.addEventListener('click', sync);
        connectSse();
        render();
    }

    function onConnection(callback) {
        state.listeners.push(callback);
        callback(state.connection);
    }

    function setConnection(value) {
        state.connection = value;
        state.listeners.forEach((callback) => callback(value));
    }

    async function sync() {
        try {
            const res = await fetch(`${window.API_BASE}/approvals`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            state.tasks = await res.json();
            setConnection('online');
            render();
        } catch (error) {
            setConnection('offline');
            toast(`审批同步失败: ${error.message}`, 'error');
        }
    }

    function connectSse() {
        const es = new EventSource(`${window.API_BASE}/approvals/stream`);
        es.onopen = () => setConnection('online');
        es.onerror = () => {
            setConnection('offline');
            es.close();
            setTimeout(connectSse, 4000);
        };
        es.onmessage = (event) => {
            const task = tryParseJson(event.data);
            if (!task || task.id === '__heartbeat__') return;
            upsert(task);
        };
    }

    function upsert(task) {
        const index = state.tasks.findIndex((item) => item.id === task.id);
        if (index >= 0) {
            state.tasks.splice(index, 1, task);
        } else {
            state.tasks.unshift(task);
        }
        render();
    }

    function render() {
        if (!dom.list) return;
        const tasks = state.tasks.filter((task) => task.id !== '__heartbeat__');
        const pendingCount = tasks.filter((task) => task.status === 'PENDING').length;
        if (dom.count) dom.count.textContent = String(pendingCount);
        dom.panel?.classList.toggle('has-pending', pendingCount > 0);
        if (!tasks.length) {
            dom.list.innerHTML = '<div class="empty-list">暂无审批任务。</div>';
            return;
        }
        dom.list.innerHTML = tasks.map(renderTask).join('');
        dom.list.querySelectorAll('[data-action]').forEach((button) => {
            button.addEventListener('click', () => decide(button.dataset.id, button.dataset.action));
        });
    }

    function renderTask(task) {
        const canAct = task.status === 'PENDING';
        return `
            <article class="approval-item">
                <div class="approval-main">
                    <div>
                        <div class="approval-title">${escapeHtml(task.toolName || task.alertKey || 'approval')}</div>
                        <div class="approval-meta">${escapeHtml(task.dataId || '-')}${task.group ? ' · ' + escapeHtml(task.group) : ''}</div>
                        ${task.runId ? `<div class="approval-meta">run ${escapeHtml(task.runId)}</div>` : ''}
                    </div>
                    <span class="status ${escapeAttr(task.status)}">${escapeHtml(task.status || '-')}</span>
                </div>
                <pre>${escapeHtml(task.reasoning || task.errorMessage || '')}</pre>
                <div class="approval-actions">
                    <button class="btn secondary" data-id="${escapeAttr(task.id)}" data-action="approve" ${canAct ? '' : 'disabled'}>通过</button>
                    <button class="btn secondary danger" data-id="${escapeAttr(task.id)}" data-action="reject" ${canAct ? '' : 'disabled'}>拒绝</button>
                </div>
            </article>
        `;
    }

    async function decide(id, action) {
        try {
            const res = await fetch(`${window.API_BASE}/approvals/${encodeURIComponent(id)}/${action}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: action === 'reject' ? JSON.stringify({ reason: '前端拒绝' }) : '{}',
            });
            const body = await res.json();
            if (!res.ok || body.status === 'error') {
                toast(body.message || `审批失败: HTTP ${res.status}`, 'error');
                return;
            }
            upsert(body);
            toast(action === 'approve' ? '已通过审批。' : '已拒绝审批。', 'success');
        } catch (error) {
            toast(`审批失败: ${error.message}`, 'error');
        }
    }

    function toast(message, kind = 'info') {
        if (!dom.toast) return;
        dom.toast.textContent = message;
        dom.toast.className = `toast ${kind} show`;
        clearTimeout(toast.timer);
        toast.timer = setTimeout(() => {
            dom.toast.className = 'toast';
        }, 3200);
    }

    function tryParseJson(text) {
        try {
            return JSON.parse(text);
        } catch {
            return null;
        }
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

    return { init, sync, onConnection, toast };
})();
