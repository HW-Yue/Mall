package com.yue.common.log.sentinel;

import com.alibaba.csp.sentinel.node.ClusterNode;
import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
import com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;

/**
 * 把 Sentinel 的 ClusterNode 统计暴露到 Micrometer（/actuator/prometheus）。
 * <p>指标命名与社区 Grafana Dashboard ID=10631 保持一致，直接复用面板：
 * <ul>
 *     <li>{@code sentinel_pass_qps} - 每秒通过请求数</li>
 *     <li>{@code sentinel_block_qps} - 每秒被限流/熔断请求数</li>
 *     <li>{@code sentinel_success_qps} - 每秒成功请求数</li>
 *     <li>{@code sentinel_exception_qps} - 每秒业务异常数</li>
 *     <li>{@code sentinel_rt} - 平均 RT（毫秒）</li>
 *     <li>{@code sentinel_current_thread} - 当前占用线程数</li>
 * </ul>
 * <p>后台线程周期性扫描 {@link ClusterBuilderSlot#getClusterNodeMap()}，发现新资源即注册 Gauge；
 * Gauge 每次 Prometheus 抓取时读取实时值。
 */
@Slf4j
public class SentinelMetricsBinder implements MeterBinder {

    private final SentinelCommonProperties properties;
    private final String applicationName;

    private MeterRegistry registry;
    private ScheduledExecutorService scheduler;
    /** 已经注册过 Gauge 的资源名（避免重复注册） */
    private final Set<String> boundResources = ConcurrentHashMap.newKeySet();

    public SentinelMetricsBinder(SentinelCommonProperties properties, String applicationName) {
        this.properties = properties;
        this.applicationName = applicationName == null || applicationName.isBlank() ? "application" : applicationName;
    }

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        this.registry = meterRegistry;
    }

    @PostConstruct
    public void start() {
        if (!properties.isMetricsEnabled()) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sentinel-metrics-binder");
            t.setDaemon(true);
            return t;
        });
        long interval = Math.max(200L, properties.getMetricsIntervalMs());
        scheduler.scheduleAtFixedRate(this::scanAndRegister, interval, interval, TimeUnit.MILLISECONDS);
        log.info("[Sentinel] MeterBinder started, intervalMs={}, app={}", interval, applicationName);
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void scanAndRegister() {
        if (registry == null) {
            return;
        }
        try {
            Map<ResourceWrapper, ClusterNode> nodes = ClusterBuilderSlot.getClusterNodeMap();
            if (nodes == null || nodes.isEmpty()) {
                return;
            }
            for (Map.Entry<ResourceWrapper, ClusterNode> entry : nodes.entrySet()) {
                String resource = entry.getKey().getName();
                if (resource == null || !boundResources.add(resource)) {
                    continue;
                }
                bindGauges(resource, entry.getValue());
            }
        } catch (Throwable ex) {
            log.warn("[Sentinel] scan cluster nodes failed: {}", ex.getMessage());
        }
    }

    private void bindGauges(String resource, ClusterNode node) {
        Tags tags = Tags.of("app", applicationName, "resource", resource);

        registerGauge("sentinel_pass_qps", tags, node, ClusterNode::passQps);
        registerGauge("sentinel_block_qps", tags, node, ClusterNode::blockQps);
        registerGauge("sentinel_success_qps", tags, node, ClusterNode::successQps);
        registerGauge("sentinel_exception_qps", tags, node, ClusterNode::exceptionQps);
        registerGauge("sentinel_rt", tags, node, ClusterNode::avgRt);
        registerGauge("sentinel_current_thread", tags, node, n -> (double) n.curThreadNum());
        registerGauge("sentinel_total_qps", tags, node, ClusterNode::totalQps);

        log.debug("[Sentinel] metric gauges bound for resource={}", resource);
    }

    private void registerGauge(String name, Tags tags, ClusterNode node, ToDoubleFunction<ClusterNode> fn) {
        Gauge.builder(name, node, n -> safeGet(fn, n))
                .tags(tags)
                .register(registry);
    }

    private static double safeGet(ToDoubleFunction<ClusterNode> fn, ClusterNode n) {
        if (n == null) {
            return 0d;
        }
        try {
            double v = fn.applyAsDouble(n);
            return Double.isFinite(v) ? v : 0d;
        } catch (Throwable ignore) {
            return 0d;
        }
    }
}
