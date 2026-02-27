package ru.chessinsight.infrastructure.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private static final String ACQUIRED_ATTR = "moveAnalysisAdmission.acquired";

    private final Semaphore limiter;
    private final long acquireTimeoutMs;
    private final ObjectMapper objectMapper;

    public MoveAnalysisAdmissionInterceptor(
            ObjectMapper objectMapper,
            @Value("${analysis.move.http.max-concurrent-requests:48}") int maxConcurrentRequests,
            @Value("${analysis.move.http.acquire-timeout-ms:500}") long acquireTimeoutMs
    ) {
        this.objectMapper = objectMapper;
        this.limiter = new Semaphore(Math.max(1, maxConcurrentRequests), true);
        this.acquireTimeoutMs = Math.max(1L, acquireTimeoutMs);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        boolean acquired;
        try {
            acquired = limiter.tryAcquire(acquireTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writeTooManyRequests(response, request.getRequestURI(), "Interrupted while waiting for processing slot");
            return false;
        }

        if (!acquired) {
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
