package com.cts.fundtrack.dgcs.exception;

//import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * Centralized exception handling configuration for the FundTrack application.
 * <p>
 * This class intercepts exceptions thrown by Controllers and maps them to
 * a consistent {@link ErrorResponse} format with appropriate HTTP status codes.
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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

    /**
     * Handles validation errors from {@code @Valid} body objects.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "Invalid value" : fe.getDefaultMessage(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ))
                .entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("; "));

        log.debug("Validation failed at {}: {}", getPath(request), details);
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed: " + details, getPath(request));
    }

    /**
     * Handles Constraint Violations (e.g., URL parameter validation).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        String details = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, "Constraint violation: " + details, getPath(request));
    }

    /**
     * Handles database integrity issues like duplicate keys.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                      HttpServletRequest request) {
        log.warn("Data integrity violation at {}: {}", getPath(request), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Email or phone number already exists", getPath(request));
    }

    /**
     * Handles custom duplicate resource logic.
     */
    @ExceptionHandler({DuplicateResourceException.class, DuplicateTransactionException.class})
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex,
                                                         HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), getPath(request));
    }

    /**
     * Handles Resource/User Not Found scenarios.
     */
    @ExceptionHandler({UserNotFoundException.class, EntityIdNotFoundException.class, ProgramNotFoundException.class,
            ApplicationNotFoundException.class, ApproverNotFoundException.class, ReviewerNotFoundException.class,
            ResourceNotFoundException.class, NotificationNotFoundException.class, ReviewNotFoundException.class, RecommendationNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest request) {

        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), getPath(request));
    }

    /**
     * Handles malformed JSON or missing request bodies.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMissingBody(HttpMessageNotReadableException ex,
                                                           HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Request body is missing or malformed", getPath(request));
    }

//    /**
//     * Handles JWT security expiration.
//     */
//    @ExceptionHandler({ExpiredJwtException.class, TokenExpiredException.class})
//    public ResponseEntity<ErrorResponse> handleExpiredTokens(RuntimeException ex, HttpServletRequest request) {
//        log.warn("Security token expired: {}", ex.getMessage());
//        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), getPath(request));
//    }

    /**
     * Handles invalid authentication tokens.
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex,
                                                            HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), getPath(request));
    }

    /**
     * Handles user session state conflicts.
     */
    @ExceptionHandler({UserAlreadyLoggedInException.class, UserAlreadyLoggedOutException.class})
    public ResponseEntity<ErrorResponse> handleSessionConflicts(RuntimeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), getPath(request));
    }

    /**
     * Handles logic processing and validation failures.
     */
    @ExceptionHandler({InvalidInputException.class, DecisionProcessingException.class,
            ApplicationValidationException.class, InvalidProgramStateException.class})
    public ResponseEntity<ErrorResponse> handleProcessingExceptions(RuntimeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), getPath(request));
    }

    /**
     * Handles unauthorized access attempts.
     */
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccessException(UnauthorizedAccessException e,
                                                                           HttpServletRequest req) {
        return buildResponse(HttpStatus.FORBIDDEN, e.getMessage(), getPath(req));
    }

    /**
     * Handles security encryption and decryption failures.
     */
    @ExceptionHandler(EncryptionException.class)
    public ResponseEntity<ErrorResponse> handleEncryptionException(EncryptionException ex,
                                                                   HttpServletRequest request) {
        log.error("Security/Encryption error at {}: {}", getPath(request), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), getPath(request));
    }
    /**
     * Handles Spring Security AccessDeniedException.
     * <p>
     * This specifically catches errors from @PreAuthorize annotations when an authenticated
     * user lacks the required roles (e.g., trying to access an ADMIN route with USER role).
     * </p>
     *
     * @param ex  the access denied exception
     * @param req the current web request
     * @return a structured 403 Forbidden response
     */
//    @ExceptionHandler(AccessDeniedException.class)
//    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
//        log.warn("Access Denied for path {}: {}", getPath(req), ex.getMessage());
//        return buildResponse(HttpStatus.FORBIDDEN,
//                "You do not have the necessary permissions to perform this action.",
//                getPath(req));
//    }
    /**
     * Handles compliance-specific rule violations.
     * Maps to 422 Unprocessable Entity as the request is well-formed but violates business policy.
     */


    // ---------------------------------------------------------
    // MODULE 7 :- Compliance & Post-Grant Reporting
    // ---------------------------------------------------------

    /**
     * Handles FileStorageException.
     * Triggered when the system cannot write the PDF to the disk.
     */

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorageException(FileStorageException ex,
                                                                    HttpServletRequest request) {
        // Log as ERROR because disk issues usually require admin intervention
        log.error("Storage System Failure: {} | Requested Path: {}", ex.getMessage(), request.getRequestURI());

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, // 500 is correct for server-side IO issues
                "File storage service is currently unavailable: " + ex.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * Handles compliance violations related to the reporting timeline.
     * Returns 403 Forbidden because the user is barred from this action
     * due to regulatory constraints.
     */
    @ExceptionHandler(ReportingWindowException.class)
    public ResponseEntity<ErrorResponse> handleReportingWindowException(ReportingWindowException ex,
                                                                        HttpServletRequest request) {
        log.warn("Compliance Violation | Reporting Window Breached | Path: {} | Message: {}",
                request.getRequestURI(), ex.getMessage());

        return buildResponse(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(ReportEligibilityException.class)
    public ResponseEntity<ErrorResponse> handleReportEligibility(ReportEligibilityException ex,
                                                                 HttpServletRequest request) {
        log.warn("Report Eligibility Violation | Path: {} | Reason: {}",
                request.getRequestURI(), ex.getMessage());

        // Use your buildResponse helper to satisfy the 5-argument requirement!
        return buildResponse(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(GrantReportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReportNotFound(GrantReportNotFoundException ex,
                                                              HttpServletRequest request) {
        // You can add specific logging here just for reports
        log.warn("Report Lookup Failure | ID not found in system | Path: {}", request.getRequestURI());

        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(ReportLockedException.class)
    public ResponseEntity<ErrorResponse> handleAuditIntegrity(ReportLockedException ex,
                                                              HttpServletRequest request) {
        log.error("Compliance Audit Conflict | Path: {} | Message: {}",
                request.getRequestURI(), ex.getMessage());

        // Using your buildResponse helper
        return buildResponse(
                HttpStatus.CONFLICT, // 409 Conflict
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(DataExportException.class)
    public ResponseEntity<ErrorResponse> handleDataExport(DataExportException ex, HttpServletRequest request) {
        // This is logged as ERROR because it's a failure of the system's ability to provide data
        log.error("Export Service Failure | Path: {} | Reason: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(ComplianceDataException.class)
    public ResponseEntity<ErrorResponse> handleComplianceData(ComplianceDataException ex, HttpServletRequest request) {

        log.error("Compliance Data Failure | Path: {} | Reason: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage(),
                request.getRequestURI()
        );
    }
    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(InvalidFileException ex, HttpServletRequest request) {
        log.warn("Invalid File Upload | Path: {} | Reason: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI()
        );
    }
    /**
     * Handles failures in persisting report metadata after file operations.
     * This indicates a synchronization failure between the storage layer and the database.
     */
    @ExceptionHandler(ReportPersistenceException.class)
    public ResponseEntity<ErrorResponse> handleReportPersistence(ReportPersistenceException ex,
                                                                 HttpServletRequest request) {
        // Log as ERROR because this implies a state mismatch (file saved but DB failed)
        log.error("Critical Sync Failure | Report metadata could not be saved: {} | Path: {}",
                ex.getMessage(), request.getRequestURI());

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The report file was processed, but the system failed to update the record. " + ex.getMessage(),
                request.getRequestURI()
        );
    }
    /**
     * Handles authentication failures due to incorrect credentials.
     * This is triggered when the password does not match the email in the database.
     */
//    @ExceptionHandler(BadCredentialsException.class)
//    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex,
//                                                              HttpServletRequest request) {
//        // Log as WARN to monitor for potential brute-force attempts
//        log.warn("Authentication Failed | Invalid credentials for request: {}",
//                request.getRequestURI());
//
//        return buildResponse(
//                HttpStatus.UNAUTHORIZED,
//                "Invalid email or password. Please check your credentials and try again.",
//                request.getRequestURI()
//        );
//    }
//}
}