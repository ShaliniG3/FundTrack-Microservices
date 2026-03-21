package com.fundtrack.analytics_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler
{
    /**
     * Helper to build a standardized error response entity.
     */
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, String path) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .build();
        return ResponseEntity.status(status).body(body);
    }
    private String getPath(HttpServletRequest request) {
        return (request != null) ? request.getRequestURI() : "";
    }

    /**
     * Handles general unexpected server exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception e, HttpServletRequest req) {
        log.error("Unhandled exception: ", e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal server error occurred, please contact management.",
                getPath(req));
    }
    @ExceptionHandler(ProgramNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        log.info("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), getPath(request));
    }

}
