(() => {
    const params = new URLSearchParams(location.search);
    const fromQuery = params.get('api');
    const fromStorage = localStorage.getItem('ops-spring-ai.api-base');
    const sameOriginApi = location.protocol === 'file:'
        ? 'http://localhost:2322/api/v1'
        : `${location.origin}/api/v1`;
    const base = fromQuery || fromStorage || sameOriginApi;
    // 默认随 jar 同源访问；经 Gateway 时使用 ?api=http://127.0.0.1:8090/gw/api/v1/ops-ai
    window.API_BASE = base.replace(/\/+$/, '');
    if (fromQuery) {
        localStorage.setItem('ops-spring-ai.api-base', window.API_BASE);
    }
})();
