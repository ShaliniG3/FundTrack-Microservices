package com.cts.fundtrack.dgcs.exception;
/**
 * Custom exception thrown when business-level validation fails for user input.
 * <p>
 * This exception is typically intercepted by the {@link GlobalExceptionHandler} 
 * to return an {@code HTTP 400 Bad Request} status. It is used for logical 
 * validation errors that standard Bean Validation (JSR-303) cannot catch, 
 * such as verifying that a program's end date is after its start date.
 * </p>
 */
public class InvalidInputException extends RuntimeException {

    /**
     * Constructs a new InvalidInputException with a specific descriptive message.
     *
     * @param message A human-readable explanation of why the validation failed.
     */
    public InvalidInputException(String message) {
        super(message);
    }
}



