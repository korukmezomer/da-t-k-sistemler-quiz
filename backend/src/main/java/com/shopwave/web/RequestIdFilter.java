package com.shopwave.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        MDC.put(RequestIdContext.MDC_KEY, requestId);
        response.setHeader(RequestIdContext.REQUEST_ID_HEADER, requestId);
        response.setHeader(RequestIdContext.CORRELATION_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RequestIdContext.MDC_KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(RequestIdContext.REQUEST_ID_HEADER);
        if (hasText(requestId)) {
            return requestId.trim();
        }

        String correlationId = request.getHeader(RequestIdContext.CORRELATION_ID_HEADER);
        if (hasText(correlationId)) {
            return correlationId.trim();
        }

        return UUID.randomUUID().toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
