package com.fundtrack.audit_service.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception interceptor that transforms standard Spring exceptions
 * and custom business exceptions into the standardized {@link ErrorResponse} format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles custom resource not found scenarios (UserIdNotFound, EntityIdNotFound).
     */
    @ExceptionHandler(EntityIdNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityIdNotFoundException ex, HttpServletRequest request) {
        return buildResponseEntity(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }
    @ExceptionHandler(UserIdNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserIdNotFoundException ex, HttpServletRequest request) {
        return buildResponseEntity(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }


    /**
     * Handles @Valid validation failures.
     * Joins multiple field errors into a single readable string.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return buildResponseEntity(HttpStatus.BAD_REQUEST, "Validation Failed: " + details, request.getRequestURI());
    }

    /**
     * Handles malformed JSON and invalid Enum types (ActionType/EntityType).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = "Malformed JSON request or invalid Enum value provided.";
        return buildResponseEntity(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    /**
     * Helper method to construct the ErrorResponse using the builder pattern.
     */
    private ResponseEntity<ErrorResponse> buildResponseEntity(HttpStatus status, String message, String path) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }
}