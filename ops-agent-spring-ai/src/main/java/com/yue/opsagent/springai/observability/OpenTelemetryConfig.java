package com.yue.opsagent.springai.observability;

import com.yue.opsagent.springai.config.OpsAiProperties;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * OTLP gRPC 导出到 SkyWalking OAP 10.4（与 {@code ops-agent} 一致）。
 */
@Configuration
@ConditionalOnProperty(prefix = "ops-ai.otlp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenTelemetryConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenTelemetryConfig.class);

    @Bean
    public OpenTelemetry openTelemetry(OpsAiProperties props) {
        var otlp = props.getOtlp();
        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(otlp.getEndpoint())
                .setTimeout(Duration.ofSeconds(otlp.getTimeoutSeconds()))
                .build();

        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.builder()
                        .put("service.name", otlp.getServiceName())
                        .put("service.version", "1.0.0")
                        .build()));

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter)
                        .setScheduleDelay(Duration.ofSeconds(1))
                        .setExporterTimeout(Duration.ofSeconds(otlp.getTimeoutSeconds()))
                        .build())
                .setResource(resource)
                .build();

        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();

        log.info("OpenTelemetry OTLP initialized: endpoint={}, serviceName={}",
                otlp.getEndpoint(), otlp.getServiceName());
        return sdk;
    }

    @Bean
    public Tracer genAiTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("spring-ai-genai", "1.0.0");
    }
}
