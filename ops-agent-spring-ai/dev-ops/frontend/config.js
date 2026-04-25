(() => {
    const params = new URLSearchParams(location.search);
    const fromQuery = params.get('api');
    const fromStorage = localStorage.getItem('ops-spring-ai.api-base');
    const base = fromQuery || fromStorage || 'http://100.86.250.112:8090/gw/api/v1/ops-ai';
    // 默认经 Gateway；直连示例 ?api=http://127.0.0.1:2322/api/v1
    window.API_BASE = base.replace(/\/+$/, '');
    if (fromQuery) {
        localStorage.setItem('ops-spring-ai.api-base', window.API_BASE);
    }
})();
