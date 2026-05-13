(() => {
    const params = new URLSearchParams(location.search);
    const fromQuery = params.get('api');
    const fromStorage = localStorage.getItem('yue-ops-agent.api-base');
    const buildGatewayBase = (host) => `${location.protocol}//${host}:8090/gw/api/v1/ops-ai`;
    const pageHost = location.hostname || '100.86.250.112';
    const defaultApi = buildGatewayBase(pageHost);
    const isLocalHost = (host) => host === 'localhost' || host === '127.0.0.1' || host === '::1';
    const normalizeLegacyBase = (value) => {
        if (!value) {
            return value;
        }
        const trimmed = value.replace(/\/+$/, '');
        try {
            const parsed = new URL(trimmed);
            const legacyOpsPath = parsed.pathname.startsWith('/gw/api/v1/ops-ai')
                || parsed.pathname.startsWith('/api/v1');
            if (!isLocalHost(pageHost) && isLocalHost(parsed.hostname) && legacyOpsPath) {
                return defaultApi;
            }
        } catch (_) {
            return trimmed;
        }
        return trimmed;
    };
    const base = normalizeLegacyBase(fromQuery) || normalizeLegacyBase(fromStorage) || defaultApi;
    window.API_BASE = base.replace(/\/+$/, '');
    if (fromQuery || fromStorage !== window.API_BASE) {
        localStorage.setItem('yue-ops-agent.api-base', window.API_BASE);
    }
})();
