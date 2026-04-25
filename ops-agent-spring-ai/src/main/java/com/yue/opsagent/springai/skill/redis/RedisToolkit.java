package com.yue.opsagent.springai.skill.redis;

import com.yue.opsagent.springai.config.OpsAiProperties;
import com.yue.opsagent.springai.skill.api.ToolResult;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis 只读诊断（Lettuce）。
 */
@Component
public class RedisToolkit {

    private final OpsAiProperties props;

    public RedisToolkit(OpsAiProperties props) {
        this.props = props;
    }

    private RedisURI uri() {
        var r = props.getRedis();
        if (r.getUri() != null && !r.getUri().isBlank()) {
            return RedisURI.create(r.getUri().trim());
        }
        RedisURI.Builder b = RedisURI.builder()
                .withHost(r.getHost())
                .withPort(r.getPort())
                .withDatabase(r.getDatabase());
        if (r.getPassword() != null && !r.getPassword().isBlank()) {
            b.withPassword(r.getPassword().toCharArray());
        }
        return b.build();
    }

    private <T> T withSync(java.util.function.Function<RedisCommands<String, String>, T> fn) {
        RedisClient client = RedisClient.create(uri());
        try (StatefulRedisConnection<String, String> conn = client.connect()) {
            return fn.apply(conn.sync());
        } finally {
            client.shutdown();
        }
    }

    public ToolResult info(String section) {
        try {
            String sec = (section == null || section.isBlank()) ? "default" : section.trim();
            String out = withSync(s -> "default".equals(sec) ? s.info() : s.info(sec));
            return ToolResult.ok("info", truncate(out, 80_000));
        } catch (Exception e) {
            return ToolResult.error("redis_info 失败: " + e.getMessage());
        }
    }

    public ToolResult slowlog(int count) {
        int n = count <= 0 ? 32 : Math.min(count, 128);
        try {
            List<Object> raw = withSync(s -> s.slowlogGet(n));
            return ToolResult.ok("slowlog", raw == null ? List.of() : raw);
        } catch (Exception e) {
            return ToolResult.error("redis_slowlog 失败: " + e.getMessage());
        }
    }

    public ToolResult clientList() {
        try {
            String cl = withSync(RedisCommands::clientList);
            return ToolResult.ok("client_list", truncate(cl, 80_000));
        } catch (Exception e) {
            return ToolResult.error("redis_client_list 失败: " + e.getMessage());
        }
    }

    public ToolResult memoryStats() {
        try {
            String mem = withSync(s -> s.info("memory"));
            return ToolResult.ok("memory_info", truncate(mem, 80_000));
        } catch (Exception e) {
            return ToolResult.error("redis_memory 失败: " + e.getMessage());
        }
    }

    public ToolResult getKey(String key, String scanPattern, int scanCount) {
        try {
            if (key != null && !key.isBlank()) {
                String v = withSync(s -> s.get(key.trim()));
                return ToolResult.ok("get", Map.of("key", key, "value", v == null ? "" : truncate(v, 16_000)));
            }
            String pattern = (scanPattern == null || scanPattern.isBlank()) ? "*" : scanPattern;
            int count = scanCount <= 0 ? 50 : Math.min(scanCount, 500);
            return ToolResult.ok("scan_sample", scanSample(pattern, count));
        } catch (Exception e) {
            return ToolResult.error("redis_get 失败: " + e.getMessage());
        }
    }

    private List<String> scanSample(String pattern, int maxKeys) {
        return withSync(s -> {
            List<String> keys = new ArrayList<>();
            ScanCursor cursor = ScanCursor.INITIAL;
            ScanArgs args = ScanArgs.Builder.matches(pattern).limit(50);
            while (true) {
                KeyScanCursor<String> next = s.scan(cursor, args);
                for (String k : next.getKeys()) {
                    keys.add(k);
                    if (keys.size() >= maxKeys) {
                        return keys;
                    }
                }
                cursor = next;
                if (next.isFinished()) {
                    break;
                }
            }
            return keys;
        });
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "\n...(truncated)";
    }
}
