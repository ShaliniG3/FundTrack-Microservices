package com.fundtrack.exceptions;

import com.fundtrack.modules.application_service.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Handle 404 Not Found Errors
    @ExceptionHandler({ApplicationNotFoundException.class, ProgramNotFoundException.class, ResourceNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, WebRequest request) {
        return buildResponse(ex, request, HttpStatus.NOT_FOUND);
    }

    // Handle 409 Conflict Errors (Duplicate submissions)
    @ExceptionHandler(DuplicateApplicationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DuplicateApplicationException ex, WebRequest request) {
        return buildResponse(ex, request, HttpStatus.CONFLICT);
    }

    // Handle 400 Bad Request (Invalid state or file type)
    @ExceptionHandler({UnsupportedDocumentTypeException.class, InvalidApplicationStateException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, WebRequest request) {
        return buildResponse(ex, request, HttpStatus.BAD_REQUEST);
    }

    // Handle 500 Internal Server Error (Anything else)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            "An unexpected server error occurred. Please contact support.",
            request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Helper method to keep code DRY (Don't Repeat Yourself)
    private ResponseEntity<ErrorResponse> buildResponse(Exception ex, WebRequest request, HttpStatus status) {
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            ex.getMessage(),
            request.getDescription(false)
        );
        return new ResponseEntity<>(error, status);
    }

    @ExceptionHandler(ApplicationNotSubmittedException.class)
    public ResponseEntity<ErrorResponse> handleNotSubmitted(ApplicationNotSubmittedException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            ex.getMessage(),
            request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.PRECONDITION_FAILED); // 412 status
    }

    @ExceptionHandler(InvalidReviewScoreException.class)
    public ResponseEntity<ErrorResponse> handleInvalidScore(InvalidReviewScoreException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            ex.getMessage(),
            request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST); // 400 status
    }

    @ExceptionHandler(ApplicationNotReadyForDecisionException.class)
    public ResponseEntity<ErrorResponse> handleNotReady(ApplicationNotReadyForDecisionException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            ex.getMessage(),
            request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.PRECONDITION_FAILED); // 412 status
    }

    @ExceptionHandler(InvalidDecisionTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDecision(InvalidDecisionTypeException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            ex.getMessage(),
            request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST); // 400 status
    }


}