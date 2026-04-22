(() => {
    const params = new URLSearchParams(location.search);
    const fromQuery = params.get('api');
    const fromStorage = localStorage.getItem('ops-agent.api-base');
    const base = fromQuery || fromStorage || 'http://localhost:8098';
    window.API_BASE = base.replace(/\/+$/, '');
    if (fromQuery) {
        localStorage.setItem('ops-agent.api-base', window.API_BASE);
    }
})();
