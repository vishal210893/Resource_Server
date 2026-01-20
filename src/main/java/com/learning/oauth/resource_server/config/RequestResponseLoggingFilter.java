package com.learning.oauth.resource_server.config;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Order(1) // Ensure it runs early
public class RequestResponseLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final int MAX_PAYLOAD_LENGTH = 10000; // Max characters of payload to log
    private final ObjectMapper objectMapper;

    public RequestResponseLoggingFilter() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse httpResponse)) {
            filterChain.doFilter(request, response);
            return;
        }

        String logId = UUID.randomUUID().toString().substring(0, 8);

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest,0);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long timeTaken = System.currentTimeMillis() - startTime;
            logRequest(requestWrapper, timeTaken, logId);
            logResponse(responseWrapper, logId);
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request, long timeTaken, String logId) {
        StringBuilder msg = new StringBuilder();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        msg.append("\n╔═══════════════════════════ REQUEST START (ID: ").append(logId).append(") ═══════════════════════════╗\n");
        msg.append(String.format("║ %-18s: %s\n", "Timestamp", timestamp));
        msg.append(String.format("║ %-18s: %s\n", "Method", request.getMethod()));
        msg.append(String.format("║ %-18s: %s\n", "URI", request.getRequestURI()));
        if (request.getQueryString() != null) {
            msg.append(String.format("║ %-18s: %s\n", "QueryString", request.getQueryString()));
        }
        msg.append(String.format("║ %-18s: %s\n", "Client IP", request.getRemoteAddr()));

        msg.append("║ Headers           :\n");
        Enumeration<String> headerNames = request.getHeaderNames();
        if (!headerNames.hasMoreElements()) {
            msg.append("║                     [NONE]\n");
        } else {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                msg.append(String.format("║   %-15s: %s\n", headerName, request.getHeader(headerName)));
            }
        }

        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            String contentString = getContentString(content, request.getCharacterEncoding());
            msg.append("║ Body              :\n").append(indentMultiLine(formatPayload(contentString), "║   ")).append("\n");
        } else {
            msg.append(String.format("║ %-18s: %s\n", "Body", "[EMPTY]"));
        }
        msg.append(String.format("║ %-18s: %d ms\n", "Processing Time", timeTaken));
        msg.append("╚════════════════════════════ REQUEST END (ID: ").append(logId).append(") ═════════════════════════════╝"); // No newline at the very end
        log.info(msg.toString());
    }

    private void logResponse(ContentCachingResponseWrapper response, String logId) {
        StringBuilder msg = new StringBuilder();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        msg.append("\n╔═══════════════════════════ RESPONSE START (ID: ").append(logId).append(") ══════════════════════════╗\n");
        msg.append(String.format("║ %-18s: %s\n", "Timestamp", timestamp));
        msg.append(String.format("║ %-18s: %d\n", "Status", response.getStatus()));

        msg.append("║ Headers           :\n");
        Collection<String> headerNames = response.getHeaderNames();
        if (headerNames.isEmpty()) {
            msg.append("║                     [NONE]\n");
        } else {
            for (String headerName : headerNames) {
                msg.append(String.format("║   %-15s: %s\n", headerName, response.getHeader(headerName)));
            }
        }

        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            String contentString = getContentString(content, response.getCharacterEncoding());
            msg.append("║ Body              :\n").append(indentMultiLine(formatPayload(contentString), "║   ")).append("\n");
        } else {
            msg.append(String.format("║ %-18s: %s\n", "Body", "[EMPTY]"));
        }
        msg.append("╚═══════════════════════════ RESPONSE END (ID: ").append(logId).append(") ══════════════════════════╝"); // No newline at the very end
        log.info(msg.toString());
    }

    private String getContentString(byte[] content, String characterEncoding) {
        if (content == null || content.length == 0) {
            return "";
        }
        try {
            return new String(content, characterEncoding != null ? characterEncoding : StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to parse payload with encoding '{}', falling back to UTF-8: {}", characterEncoding, e.getMessage());
            return new String(content, StandardCharsets.UTF_8);
        }
    }

    private String formatPayload(String payload) {
        if (payload == null || payload.isEmpty()) {
            return "[EMPTY BODY]";
        }
        String trimmedPayload = payload.trim();
        if (trimmedPayload.startsWith("{") || trimmedPayload.startsWith("[")) {
            try {
                Object json = objectMapper.readValue(payload, Object.class);
                return objectMapper.writeValueAsString(json); // Already pretty-printed by ObjectMapper config
            } catch (Exception e) {
                log.warn("Attempted to pretty print JSON but failed (payload might not be valid JSON or too complex). Error: {}. Logging as is (truncated).", e.getMessage());
                // Fall through to truncate if not valid JSON
            }
        }
        return truncatePayload(payload);
    }

    private String truncatePayload(String payload) {
        if (payload.length() > MAX_PAYLOAD_LENGTH) {
            return payload.substring(0, MAX_PAYLOAD_LENGTH) + "... [TRUNCATED]";
        }
        return payload;
    }

    private String indentMultiLine(String text, String indentPrefix) {
        if (text == null || text.isEmpty() || text.equals("[EMPTY BODY]")) {
            return indentPrefix + text; // Avoid adding prefix to an already prefixed [EMPTY BODY]
        }
        return text.lines()
                .map(line -> indentPrefix + line)
                .collect(Collectors.joining("\n"));
    }
}