/*
 * 单页应用外壳：
 *  - 顶部 Tab 切换（对话 / 收件箱）
 *  - API 连通性芯片（读取 window.API_BASE）
 *  - 把 Inbox 的 pending count / SSE 状态映射到 Tab 徽章 & 状态灯
 *
 * 每个 view 的内部逻辑都在各自模块里：
 *    js/chat.js   → window.Chat.init()
 *    js/inbox.js  → window.Inbox.init()
 */
(() => {
    const tabs = document.querySelectorAll('.tab[data-view]');
    const views = document.querySelectorAll('.view');
    const pendingBadge = document.getElementById('pending-badge');
    const apiChip = document.getElementById('api-chip');
    const apiText = document.getElementById('api-text');

    function switchTo(name) {
        tabs.forEach((t) => t.classList.toggle('active', t.dataset.view === name));
        views.forEach((v) => v.classList.toggle('active', v.id === `view-${name}`));
        try { localStorage.setItem('ops-agent.active-view', name); } catch (_) {}
        if (name === 'chat') {
            window.Chat?.focus?.();
        }
    }

    tabs.forEach((t) => t.addEventListener('click', () => switchTo(t.dataset.view)));

    // ---- 恢复上次 tab / URL hash 指定 ----
    const fromHash = (location.hash || '').replace('#', '').trim();
    const stored = (() => { try { return localStorage.getItem('ops-agent.active-view'); } catch (_) { return null; } })();
    const initial = ['chat', 'inbox'].includes(fromHash) ? fromHash
                  : (stored === 'chat' || stored === 'inbox') ? stored
                  : 'chat';
    switchTo(initial);

    // ---- API 状态显示 ----
    apiText.textContent = window.API_BASE || '(未配置)';
    apiChip.title = `后端：${window.API_BASE}\n通过 ?api=... 或 localStorage['ops-agent.api-base'] 覆盖`;

    // ---- 初始化各 view ----
    window.Chat?.init();
    window.Inbox?.init();

    // ---- 连接状态 → api-chip 小圆点 ----
    window.Inbox?.onConnection((state) => {
        apiChip.classList.remove('online', 'offline');
        apiChip.classList.add(state === 'online' ? 'online' : 'offline');
    });

    // ---- pending count → tab 徽章 ----
    window.Inbox?.onPending((count) => {
        if (count > 0) {
            pendingBadge.textContent = count > 99 ? '99+' : String(count);
            pendingBadge.hidden = false;
        } else {
            pendingBadge.hidden = true;
        }
    });
})();
