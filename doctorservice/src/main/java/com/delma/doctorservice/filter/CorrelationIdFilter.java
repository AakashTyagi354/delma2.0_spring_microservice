package com.delma.doctorservice.filter;



import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// ─────────────────────────────────────────────────────────────────────────────
// CorrelationIdFilter
//
// Runs on EVERY request before anything else (@Order(1)).
//
// What it does:
//   1. Reads X-Correlation-Id header from incoming request
//      (Gateway sets this for every request)
//   2. If not present — generates a new UUID (fallback for direct calls)
//   3. Puts it in MDC (Mapped Diagnostic Context)
//      MDC is a thread-local map that Logback reads automatically
//      Every log.info() on this thread includes correlationId in JSON output
//   4. Adds correlationId to response header
//      so the caller (frontend/client) can reference it for support
//   5. Removes from MDC in finally block
//      CRITICAL — threads are reused in Spring (thread pool)
//      Without cleanup, next request on this thread gets old correlationId
// ─────────────────────────────────────────────────────────────────────────────

@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC    = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(CORRELATION_ID_MDC, correlationId);
        response.addHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC);
        }
    }
}