(() => {
    const params = new URLSearchParams(location.search);
    const fromQuery = params.get('api');
    const fromStorage = localStorage.getItem('yue-ops-agent.api-base');
    const defaultApi = 'http://localhost:8090/gw/api/v1/ops-ai';
    const base = fromQuery || fromStorage || defaultApi;
    window.API_BASE = base.replace(/\/+$/, '');
    if (fromQuery) {
        localStorage.setItem('yue-ops-agent.api-base', window.API_BASE);
    }
})();
