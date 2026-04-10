package com.cts.fundtrack.dgcs.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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

    // --- 404 NOT FOUND ---
    @ExceptionHandler({
            ApplicationNotFoundException.class,
            DisbursementNotFoundException.class,
            GrantReportNotFoundException.class,
            PaymentNotFoundException.class,
            ProgramNotFoundException.class,
            ResourceNotFoundException.class,

    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), getPath(request));
    }

    // --- 400 BAD REQUEST ---
    @ExceptionHandler({
            InvalidInputException.class,
            InvalidFileException.class,
            EncryptionException.class,
            InvalidProgramStateException.class,

    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        log.warn("Bad Request at {}: {}", getPath(request), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), getPath(request));
    }

    // --- 403 FORBIDDEN ---
    @ExceptionHandler({
            ReportingWindowException.class,
            ReportEligibilityException.class,
            ProgramLifecycleException.class,
            ComplianceViolationException.class,
    })
    public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException ex, HttpServletRequest request) {
        log.warn("Access/Policy Violation at {}: {}", getPath(request), ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), getPath(request));
    }

    // --- 409 CONFLICT ---
    @ExceptionHandler({
            DuplicateTransactionException.class,
            ReportLockedException.class,

    })
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex, HttpServletRequest request) {
        log.warn("Conflict detected at {}: {}", getPath(request), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), getPath(request));
    }

    // --- 500 INTERNAL SERVER ERROR ---
    @ExceptionHandler({
            DisbursementPersistenceException.class,
            ReportPersistenceException.class,
            FileStorageException.class,
            ComplianceDataException.class,
            DataExportException.class,
            ReceiptGenerationException.class
    })
    public ResponseEntity<ErrorResponse> handleInternalServerErrors(RuntimeException ex, HttpServletRequest request) {
        log.error("System Failure at {}: {}", getPath(request), ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal system error: " + ex.getMessage(), getPath(request));
    }

    // --- 503 SERVICE UNAVAILABLE ---
    @ExceptionHandler({
            DisbursementDataAccessException.class
    })
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(RuntimeException ex, HttpServletRequest request) {
        log.error("Service linkage failure at {}: {}", getPath(request), ex.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Remote service or database is temporarily unavailable.", getPath(request));
    }

    // --- SPRING STANDARD EXCEPTION HANDLERS ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "Invalid value" : fe.getDefaultMessage(),
                        (a, b) -> a, LinkedHashMap::new))
                .entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed: " + details, getPath(request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String details = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, "Constraint violation: " + details, getPath(request));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "Database integrity violation (e.g., duplicate entry).", getPath(request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed JSON or missing request body.", getPath(request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobal(Exception e, HttpServletRequest req) {
        log.error("Uncaught exception: ", e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", getPath(req));
    }
}