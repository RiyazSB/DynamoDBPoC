package com.example.DynamoDBPOC.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "app.logging.requests.enabled", havingValue = "true", matchIfMissing = true)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip logging for swagger/openapi/actuator paths
        String path = request.getRequestURI();
        if (isSkippedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 10000);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            String method = request.getMethod();
            String requestBody = getBody(wrappedRequest.getContentAsByteArray());
            String responseBody = getBody(wrappedResponse.getContentAsByteArray());
            int status = wrappedResponse.getStatus();
            String clientIp = getClientIp(request);

            log.info("[REQUEST]  {} {} | IP: {}{}",
                    method, path, clientIp, formatBodySegment(requestBody));
            log.info("[RESPONSE] {} {} | Status: {} | Duration: {}ms{}",
                    method, path, status, duration, formatBodySegment(responseBody));

            // IMPORTANT: copy body back so it is sent to client
            wrappedResponse.copyBodyToResponse();
        }
    }

    private String formatBodySegment(String body) {
        return "(empty)".equals(body) ? "" : " | Body: " + body;
    }

    private String getBody(byte[] content) {
        if (content == null || content.length == 0) return "(empty)";
        String body = new String(content, StandardCharsets.UTF_8);
        // Truncate at 2000 chars to avoid huge log entries
        return body.length() > 2000 ? body.substring(0, 2000) + "...[truncated]" : body;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isEmpty())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }

    private boolean isSkippedPath(String path) {
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator")
                || path.startsWith("/webjars");
    }
}



