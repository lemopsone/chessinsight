package ru.chessinsight.infrastructure.web.config;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ObservabilityStartupDiagnostics implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityStartupDiagnostics.class);

    private final Environment environment;
    private final List<SpanExporter> spanExporters;

    public ObservabilityStartupDiagnostics(Environment environment, List<SpanExporter> spanExporters) {
        this.environment = environment;
        this.spanExporters = spanExporters;
    }

    @Override
    public void run(ApplicationArguments args) {
        String tracingEnabled = environment.getProperty("management.tracing.enabled", "true");
        String otlpEndpoint = environment.getProperty("management.otlp.tracing.endpoint", "<not-set>");
        String otlpTransport = environment.getProperty("management.otlp.tracing.transport", "http");

        log.info(
                "observability.startup tracingEnabled={} otlpEndpoint={} transport={} spanExporters={}",
                tracingEnabled,
                otlpEndpoint,
                otlpTransport,
                spanExporters.size()
        );
        if (log.isDebugEnabled()) {
            log.debug("observability.startup spanExporterBeans={}", spanExporters.stream()
                    .map(exporter -> exporter.getClass().getName())
                    .toList());
        }
    }
}
