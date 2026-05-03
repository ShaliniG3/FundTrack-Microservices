package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when an application fails one or more business-level validation checks
 * during the submission or update process.
 *
 * <p>This covers logical validation failures that standard Bean Validation (JSR-303)
 * cannot catch — for example, when required documents are missing, eligibility rule
 * expressions evaluate to false, or application data fails cross-field checks.</p>
 *
 * <p>Mapped to {@code HTTP 400 Bad Request} by the {@link GlobalExceptionHandler}.</p>
 */
public class ApplicationValidationException extends RuntimeException {

    /**
     * Constructs the exception with a descriptive message explaining which
     * validation constraint was violated.
     *
     * @param message human-readable description of the validation failure
     */
    public ApplicationValidationException(String message) {
        super(message);
    }
}
