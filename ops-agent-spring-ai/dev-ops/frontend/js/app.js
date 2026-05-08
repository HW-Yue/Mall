(() => {
    const apiChip = document.getElementById('api-chip');
    const apiText = document.getElementById('api-text');

    function setConnection(state) {
        apiChip.classList.toggle('online', state === 'online');
        apiChip.classList.toggle('offline', state === 'offline');
    }

    apiText.textContent = window.API_BASE || '未配置 API';
    apiChip.title = `API: ${window.API_BASE}\n可用 ?api=... 覆盖`;

    function markCurrentPage() {
        const current = document.body.dataset.page || 'workbench';
        document.querySelectorAll('[data-page-link]').forEach((link) => {
            link.classList.toggle('active', link.dataset.pageLink === current);
        });
    }

    markCurrentPage();

    if (document.getElementById('route-form')) {
        window.RunWorkbench?.init();
    }
    if (document.getElementById('tool-form')) {
        window.ToolConsole?.init();
    }
    if (document.getElementById('approval-list')) {
        window.Approvals?.init();
        window.Approvals?.sync?.();
    }

    window.Approvals?.onConnection(setConnection);
})();
