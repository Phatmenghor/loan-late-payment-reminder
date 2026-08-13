package com.backend.shared.logging;

import com.backend.shared.utils.ClientIpUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Filter responsible for:
 *  - Resolving or generating traceId (X-Trace-ID / X-Request-ID / X-Trade-ID)
 *  - Populating MDC context with: traceId, clientIp, method, path
 *  - Propagating trace headers to HTTP response
 *  - Clearing MDC after request completion
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_KEY = "traceId";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String TRADE_ID_HEADER = "X-Trade-ID";

    private static final Set<String> SKIP_PATHS = Set.of(
            "/actuator/health",
            "/actuator/health/liveness",
            "/actuator/health/readiness",
            "/actuator/prometheus",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/v3/api-docs/swagger-config",
            "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        String path = request.getRequestURI();

        // Skip static asset and health check logging noise
        if (SKIP_PATHS.contains(path) || path.startsWith("/swagger-ui/") || path.startsWith("/v3/api-docs/")) {
            chain.doFilter(request, response);
            return;
        }

        String traceId = resolveOrGenerateTraceId(request);
        String clientIp = ClientIpUtils.getClientIp(request);
        long start = System.currentTimeMillis();

        MDC.put(TRACE_ID_KEY, traceId);
        MDC.put("clientIp", clientIp);
        MDC.put("method", request.getMethod());
        MDC.put("path", path);

        response.setHeader(TRACE_ID_HEADER, traceId);
        response.setHeader(REQUEST_ID_HEADER, traceId);
        response.setHeader(TRADE_ID_HEADER, traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = response.getStatus();

            if (status >= 500) {
                log.error("{} {} → {} in {}ms", request.getMethod(), path, status, duration);
            } else if (status >= 400) {
                log.warn("{} {} → {} in {}ms", request.getMethod(), path, status, duration);
            } else {
                log.info("{} {} → {} in {}ms", request.getMethod(), path, status, duration);
            }

            MDC.clear();
        }
    }

    private String resolveOrGenerateTraceId(HttpServletRequest request) {
        String incomingTraceId = request.getHeader(TRACE_ID_HEADER);
        if (incomingTraceId != null && !incomingTraceId.isBlank()) {
            return incomingTraceId.trim();
        }

        String incomingRequestId = request.getHeader(REQUEST_ID_HEADER);
        if (incomingRequestId != null && !incomingRequestId.isBlank()) {
            return incomingRequestId.trim();
        }

        String incomingTradeId = request.getHeader(TRADE_ID_HEADER);
        if (incomingTradeId != null && !incomingTradeId.isBlank()) {
            return incomingTradeId.trim();
        }

        return UUID.randomUUID().toString();
    }
}
