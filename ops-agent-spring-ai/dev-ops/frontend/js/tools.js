window.ToolConsole = (() => {
    const CATALOG = {
        docker_ops: {
            label: 'Docker',
            tools: {
                docker_logs: { args: { container: '', tail: 100 } },
                docker_stats: { args: { container: '' } },
                docker_inspect: { args: { container: '' } },
                docker_exec: { args: { container: '', command: 'pwd' } },
            },
        },
        mysql_inspect: {
            label: 'MySQL',
            tools: {
                mysql_processlist: { args: {} },
                mysql_status: { args: { pattern: 'Threads%' } },
                mysql_locks: { args: {} },
                mysql_slow_query: { args: {} },
            },
        },
        metrics_ops: {
            label: 'Prometheus',
            tools: {
                sentinel_metrics: { args: { promql: 'up' } },
                dynamictp_metrics: { args: { promql: 'process_threads' } },
                jvm_metrics: { args: { promql: 'jvm_memory_used_bytes' } },
                business_metrics: { args: { promql: 'up' } },
            },
        },
        elasticsearch_ops: {
            label: 'Elasticsearch',
            tools: {
                es_indices: { args: { cluster: 'logs' } },
                es_search_service_errors: { args: { cluster: 'logs', service: 'order-service', lookback: '1h', size: 10 } },
                es_search: { args: { cluster: 'logs', index: 'nexus-*', query: '{"query":{"match_all":{}},"size":10}' } },
                es_count: { args: { cluster: 'logs', index: 'nexus-*', query: '{"query":{"match_all":{}}}' } },
                es_aggregation: { args: { cluster: 'logs', index: 'nexus-*', body: '{"size":0,"aggs":{"by_service":{"terms":{"field":"service.keyword","size":5}}}}' } },
            },
        },
        redis_inspect: {
            label: 'Redis',
            tools: {
                redis_info: { args: { section: 'default' } },
                redis_slowlog: { args: { count: 32 } },
                redis_client_list: { args: {} },
                redis_memory: { args: {} },
                redis_get: { args: { key: '', scanPattern: '*', scanCount: 50 } },
            },
        },
        rocketmq_inspect: {
            label: 'RocketMQ',
            tools: {
                mq_topic_stats: { args: { topic: '' } },
                mq_consumer_status: { args: { consumerGroup: '', topic: '' } },
                mq_dead_letter: { args: { consumerGroup: '', topic: '' } },
            },
        },
        nacos_config: {
            label: 'Nacos',
            tools: {
                nacos_get_config: { args: { dataId: '', group: 'DEFAULT_GROUP' } },
                nacos_publish_config: { args: { dataId: '', group: 'DEFAULT_GROUP', content: '' } },
                nacos_list_instances: { args: { serviceName: '', group: 'DEFAULT_GROUP' } },
                nacos_list_services: { args: { pageNo: 1, pageSize: 50 } },
            },
        },
    };

    const dom = {};

    function init() {
        dom.skill = document.getElementById('skill-select');
        dom.tool = document.getElementById('tool-select');
        dom.args = document.getElementById('tool-args');
        dom.result = document.getElementById('tool-result');
        dom.form = document.getElementById('tool-form');
        dom.reset = document.getElementById('tool-reset');

        for (const [name, skill] of Object.entries(CATALOG)) {
            const option = document.createElement('option');
            option.value = name;
            option.textContent = `${skill.label} · ${name}`;
            dom.skill.appendChild(option);
        }

        dom.skill.addEventListener('change', renderTools);
        dom.tool.addEventListener('change', fillExample);
        dom.reset.addEventListener('click', fillExample);
        dom.form.addEventListener('submit', (event) => {
            event.preventDefault();
            execute();
        });

        renderTools();
    }

    function renderTools() {
        const skill = CATALOG[dom.skill.value];
        dom.tool.innerHTML = '';
        for (const name of Object.keys(skill.tools)) {
            const option = document.createElement('option');
            option.value = name;
            option.textContent = name;
            dom.tool.appendChild(option);
        }
        fillExample();
    }

    function fillExample() {
        const item = CATALOG[dom.skill.value].tools[dom.tool.value];
        dom.args.value = JSON.stringify(item.args, null, 2);
    }

    async function execute() {
        let args;
        try {
            args = dom.args.value.trim() ? JSON.parse(dom.args.value) : {};
        } catch (error) {
            writeResult({ status: 'error', message: `JSON 参数非法: ${error.message}` });
            return;
        }

        writeResult({ status: 'running', message: '执行中...' });
        try {
            const res = await fetch(`${window.API_BASE}/tools/execute`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    skill: dom.skill.value,
                    tool: dom.tool.value,
                    args,
                }),
            });
            const text = await res.text();
            const body = tryParseJson(text);
            if (!res.ok) {
                writeResult({ status: 'error', httpStatus: res.status, body });
                return;
            }
            writeResult(body);
            window.Approvals?.sync?.();
        } catch (error) {
            writeResult({ status: 'error', message: error.message });
        }
    }

    function writeResult(value) {
        if (typeof value === 'string') {
            dom.result.className = '';
            dom.result.innerHTML = `<pre class="tool-result-raw">${escapeHtml(value)}</pre>`;
            return;
        }
        dom.result.className = 'tool-result-rendered';
        dom.result.innerHTML = renderToolResult(value);
    }

    function renderToolResult(value) {
        const status = value.status || 'unknown';
        const message = value.message || '';
        const data = value.data;

        const statusClass = status === 'ok' || status === 'success' ? 'ok'
            : status === 'error' || status === 'failed' ? 'bad'
            : status === 'pending' ? 'warn' : '';

        let dataHtml = '';
        if (data !== undefined && data !== null) {
            if (typeof data === 'string') {
                dataHtml = `<div class="tool-result-data">
                    <div class="tool-result-data-label">输出</div>
                    <pre class="tool-result-log">${escapeHtml(data)}</pre>
                </div>`;
            } else {
                dataHtml = `<div class="tool-result-data">
                    <div class="tool-result-data-label">数据</div>
                    <pre class="tool-result-json">${escapeHtml(JSON.stringify(data, null, 2))}</pre>
                </div>`;
            }
        }

        return `
            <div class="tool-result-header">
                <span class="tool-result-status ${statusClass}">${escapeHtml(String(status).toUpperCase())}</span>
                <span class="tool-result-message">${escapeHtml(message)}</span>
            </div>
            ${dataHtml}
        `;
    }

    function tryParseJson(text) {
        try {
            return JSON.parse(text);
        } catch {
            return text;
        }
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    return { init };
})();
