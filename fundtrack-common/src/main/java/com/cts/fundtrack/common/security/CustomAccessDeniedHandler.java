package com.cts.fundtrack.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Custom {@link AccessDeniedHandler} that returns a JSON response with HTTP 403
 * when a user is authenticated but does not have sufficient permissions
 * to access a protected resource.
 * <p>
 * This handler also logs the denied access event for audit and troubleshooting
 * while avoiding sensitive information in logs.
 */
@Component
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * Handles {@code AccessDeniedException} by returning a JSON payload with HTTP 403
     * and logging the access denial event.
     *
     * @param req the {@link HttpServletRequest} for the current request
     * @param res the {@link HttpServletResponse} to write the error response
     * @param ex  the access denied exception thrown by Spring Security
     * @throws java.io.IOException if writing to the response fails
     */
    @Override
    public void handle(HttpServletRequest req,
                       HttpServletResponse res,
                       org.springframework.security.access.AccessDeniedException ex)
            throws java.io.IOException {

        // Log minimal, non-sensitive details for observability
        String path = (req != null) ? req.getRequestURI() : "unknown";
        String method = (req != null) ? req.getMethod() : "UNKNOWN";
        log.warn("Access denied: method={} path={} reason={}", method, path, ex.getMessage());

        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        res.setContentType("application/json");
        res.getWriter().write("""
        { "error": "You don't have permission to access this resource" }
        """);
    }
}

