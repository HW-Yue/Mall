(() => {
    const apiChip = document.getElementById('api-chip');
    const apiText = document.getElementById('api-text');

    function setConnection(state) {
        apiChip.classList.toggle('online', state === 'online');
        apiChip.classList.toggle('offline', state === 'offline');
    }

    apiText.textContent = window.API_BASE || '未配置 API';
    apiChip.title = `API: ${window.API_BASE}\n可用 ?api=... 覆盖`;

    window.RunWorkbench?.init();
    window.ToolConsole?.init();
    window.Approvals?.init();

    window.Approvals?.onConnection(setConnection);
    window.Approvals?.sync?.();
})();
