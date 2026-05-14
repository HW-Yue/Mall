package com.yue.opsagent.springai.skill.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.skill.api.ToolResult;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Docker Engine API（默认 unix:///var/run/docker.sock）。
 */
@Component
public class DockerToolkit {

    private final DockerClient docker;

    public DockerToolkit(OpsAiProperties props) {
        String host = props.getDocker().getHost();
        if (host == null || host.isBlank()) {
            host = "unix:///var/run/docker.sock";
        }
        var cfgBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(host);
        if (props.getDocker().getApiVersion() != null && !props.getDocker().getApiVersion().isBlank()) {
            cfgBuilder.withApiVersion(props.getDocker().getApiVersion());
        }
        DefaultDockerClientConfig config = cfgBuilder.build();
        DockerHttpClient http = new ZerodepDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();
        this.docker = DockerClientImpl.getInstance(config, http);
    }

    public ToolResult dockerLogs(String containerId, int tailLines) {
        if (containerId == null || containerId.isBlank()) {
            return ToolResult.error("container 不能为空");
        }
        int tail = tailLines <= 0 ? 100 : Math.min(tailLines, 5000);
        try {
            String resolvedContainer = resolveContainer(containerId.trim());
            StringBuilder out = new StringBuilder();
            docker.logContainerCmd(resolvedContainer)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTail(tail)
                    .exec(new ResultCallback.Adapter<>() {
                        @Override
                        public void onNext(Frame frame) {
                            out.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
                        }
                    }).awaitCompletion(45, TimeUnit.SECONDS);
            String text = out.toString();
            if (text.length() > 120_000) {
                text = text.substring(0, 120_000) + "\n...(truncated)";
            }
            return ToolResult.ok("logs", text);
        } catch (Exception e) {
            return ToolResult.error("docker_logs 失败: " + e.getMessage());
        }
    }

    public ToolResult dockerStats(String containerId) {
        if (containerId == null || containerId.isBlank()) {
            return ToolResult.error("container 不能为空");
        }
        try {
            String resolvedContainer = resolveContainer(containerId.trim());
            AtomicReference<com.github.dockerjava.api.model.Statistics> ref = new AtomicReference<>();
            var cb = new ResultCallback.Adapter<com.github.dockerjava.api.model.Statistics>() {
                @Override
                public void onNext(com.github.dockerjava.api.model.Statistics object) {
                    ref.set(object);
                }
            };
            docker.statsCmd(resolvedContainer).withNoStream(true).exec(cb);
            cb.awaitCompletion(15, TimeUnit.SECONDS);
            var s = ref.get();
            return ToolResult.ok("stats", s == null ? "no sample" : s.toString());
        } catch (Exception e) {
            return ToolResult.error("docker_stats 失败: " + e.getMessage());
        }
    }

    public ToolResult dockerInspect(String containerId) {
        if (containerId == null || containerId.isBlank()) {
            return ToolResult.error("container 不能为空");
        }
        try {
            String resolvedContainer = resolveContainer(containerId.trim());
            var insp = docker.inspectContainerCmd(resolvedContainer).exec();
            return ToolResult.ok("inspect", insp.toString());
        } catch (Exception e) {
            return ToolResult.error("docker_inspect 失败: " + e.getMessage());
        }
    }

    public ToolResult dockerExec(String containerId, String command) {
        if (containerId == null || containerId.isBlank()) {
            return ToolResult.error("container 不能为空");
        }
        if (command == null || command.isBlank()) {
            return ToolResult.error("command 不能为空");
        }
        try {
            String resolvedContainer = resolveContainer(containerId.trim());
            ExecCreateCmdResponse create = docker.execCreateCmd(resolvedContainer)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withCmd("sh", "-c", command)
                    .exec();
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            docker.execStartCmd(create.getId())
                    .exec(new com.github.dockerjava.core.command.ExecStartResultCallback(stdout, stderr))
                    .awaitCompletion(60, TimeUnit.SECONDS);
            return ToolResult.ok("exec", Map.of(
                    "stdout", stdout.toString(StandardCharsets.UTF_8),
                    "stderr", stderr.toString(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return ToolResult.error("docker_exec 失败: " + e.getMessage());
        }
    }

    /** Engine 是否可达（冒烟 / 健康检查）。 */
    public ToolResult pingEngine() {
        try {
            docker.pingCmd().exec();
            return ToolResult.ok("docker_ping", "pong");
        } catch (Exception e) {
            return ToolResult.error("docker ping 失败: " + e.getMessage());
        }
    }

    private String resolveContainer(String requested) {
        try {
            docker.inspectContainerCmd(requested).exec();
            return requested;
        } catch (Exception ignored) {
            // Fall through to service-name based lookup. Docker Java will raise the original not-found
            // semantics later if no candidate can be resolved.
        }

        List<String> candidates = new ArrayList<>();
        String plain = requested.startsWith("/") ? requested.substring(1) : requested;
        candidates.add(plain);
        candidates.add("nexus-" + plain);

        try {
            for (var container : docker.listContainersCmd().withShowAll(true).exec()) {
                if (matchesContainer(container.getNames(), candidates)
                        || matchesComposeService(container.getLabels(), plain)) {
                    return container.getId();
                }
            }
        } catch (Exception ignored) {
            return requested;
        }
        return requested;
    }

    private static boolean matchesContainer(String[] names, List<String> candidates) {
        if (names == null || names.length == 0) {
            return false;
        }
        for (String name : names) {
            if (name == null) {
                continue;
            }
            String normalized = name.startsWith("/") ? name.substring(1) : name;
            for (String candidate : candidates) {
                if (normalized.equals(candidate)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesComposeService(Map<String, String> labels, String requested) {
        if (labels == null || labels.isEmpty()) {
            return false;
        }
        return requested.equals(labels.get("com.docker.compose.service"));
    }
}
