package ru.chessinsight.infrastructure.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.chessinsight.infrastructure.web.dto.ProblemDetails;
import ru.chessinsight.infrastructure.web.exception.ProblemDetailsFactory;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class MoveAnalysisAdmissionInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(MoveAnalysisAdmissionInterceptor.class);
    private static final String ACQUIRED_ATTR = "moveAnalysisAdmission.acquired";

    private final Semaphore limiter;
    private final long acquireTimeoutMs;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public MoveAnalysisAdmissionInterceptor(
            ObjectMapper objectMapper,
            Tracer tracer,
            @Value("${analysis.move.http.max-concurrent-requests:48}") int maxConcurrentRequests,
            @Value("${analysis.move.http.acquire-timeout-ms:500}") long acquireTimeoutMs
    ) {
        this.objectMapper = objectMapper;
        this.tracer = tracer;
        this.limiter = new Semaphore(Math.max(1, maxConcurrentRequests), true);
        this.acquireTimeoutMs = Math.max(1L, acquireTimeoutMs);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Span span = tracer.nextSpan().name("move.analysis.admission.wait");
        span.tag("admission.timeout.ms", String.valueOf(acquireTimeoutMs));
        span.tag("admission.permits.available.before", String.valueOf(limiter.availablePermits()));
        boolean acquired = false;
        String result = "accepted";
        try (Tracer.SpanInScope ignored = tracer.withSpan(span.start())) {
            try {
                acquired = limiter.tryAcquire(acquireTimeoutMs, TimeUnit.MILLISECONDS);
                result = acquired ? "accepted" : "rejected";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                span.error(e);
                result = "interrupted";
                log.warn("move.analysis.admission.interrupted uri={}", request.getRequestURI());
                writeTooManyRequests(response, request.getRequestURI(), "Interrupted while waiting for processing slot");
                return false;
            }
        } finally {
            span.tag("admission.acquired", String.valueOf(acquired));
            span.tag("admission.result", result);
            span.tag("admission.permits.available.after", String.valueOf(limiter.availablePermits()));
            span.end();
        }

        if (!acquired) {
            log.warn(
                    "move.analysis.admission.rejected uri={} timeoutMs={} availablePermits={}",
                    request.getRequestURI(),
                    acquireTimeoutMs,
                    limiter.availablePermits()
            );
            writeTooManyRequests(response, request.getRequestURI(), "Service is overloaded, retry later");
            return false;
        }

        request.setAttribute(ACQUIRED_ATTR, Boolean.TRUE);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (Boolean.TRUE.equals(request.getAttribute(ACQUIRED_ATTR))) {
            limiter.release();
        }
    }

    private void writeTooManyRequests(HttpServletResponse response, String uri, String detail) throws Exception {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", "1");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ProblemDetails payload = ProblemDetailsFactory.create(HttpStatus.TOO_MANY_REQUESTS, detail, uri);
        objectMapper.writeValue(response.getOutputStream(), payload);
    }
}
