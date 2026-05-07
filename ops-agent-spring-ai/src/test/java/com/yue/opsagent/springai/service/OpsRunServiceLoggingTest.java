package com.yue.opsagent.springai.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.opsagent.springai.domain.opsroute.RouteRequest;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpsRunServiceLoggingTest {

    private OpsRunService service;
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        service = new OpsRunService(new ObjectMapper());
        logger = (Logger) LoggerFactory.getLogger(OpsRunService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void createEmitsStructuredOpsRunEventLog() {
        var session = service.create(RouteRequest.text("hello"));

        ILoggingEvent ev = findEvent(appender.list, "[OpsRunEvent]");
        assertThat(ev).isNotNull();
        assertThat(ev.getLevel()).isEqualTo(Level.INFO);

        Map<String, Object> kvs = collectStructuredFields(ev);
        assertThat(kvs).containsEntry("runId", session.runId());
        assertThat(kvs).containsEntry("eventType", "start");
        assertThat(kvs).containsKey("data");
    }

    @Test
    void toolStartLogIncludesSkillAndToolInData() {
        var session = service.create(RouteRequest.text("x"));
        appender.list.clear();

        service.toolStart(session.runId(), "metrics_ops", "metrics_query", Map.of("q", "up"));

        ILoggingEvent ev = findEventByType(appender.list, "tool_start");
        assertThat(ev).isNotNull();
        Map<String, Object> kvs = collectStructuredFields(ev);
        assertThat(kvs).containsEntry("eventType", "tool_start");
        Object data = kvs.get("data");
        assertThat(data).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = (Map<String, Object>) data;
        assertThat(dataMap).containsEntry("skill", "metrics_ops");
        assertThat(dataMap).containsEntry("tool", "metrics_query");
    }

    private static ILoggingEvent findEvent(List<ILoggingEvent> events, String marker) {
        return events.stream()
                .filter(e -> e.getFormattedMessage().startsWith(marker))
                .findFirst()
                .orElse(null);
    }

    private static ILoggingEvent findEventByType(List<ILoggingEvent> events, String eventType) {
        return events.stream()
                .filter(e -> {
                    Map<String, Object> k = collectStructuredFields(e);
                    return eventType.equals(k.get("eventType"));
                })
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> collectStructuredFields(ILoggingEvent ev) {
        java.util.LinkedHashMap<String, Object> out = new java.util.LinkedHashMap<>();
        for (Object arg : ev.getArgumentArray() == null ? new Object[0] : ev.getArgumentArray()) {
            if (arg instanceof SingleFieldAppendingMarker m) {
                out.put(m.getFieldName(), reflectFieldValue(m));
            }
        }
        return out;
    }

    private static Object reflectFieldValue(SingleFieldAppendingMarker m) {
        Class<?> c = m.getClass();
        while (c != null) {
            try {
                var f = c.getDeclaredField("fieldValue");
                f.setAccessible(true);
                return f.get(m);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }
}
