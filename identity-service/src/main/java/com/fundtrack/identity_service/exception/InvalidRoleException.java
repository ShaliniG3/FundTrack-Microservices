package com.fundtrack.identity_service.exception;

/**
 * Exception thrown when an invalid or unsupported role is provided during
 * user registration, authentication, or role-based operations.
 * <p>
 * This exception typically indicates that the supplied role does not match
 * the application's predefined role set (e.g., USER, ADMIN, APPROVER, etc.).
 */
public class InvalidRoleException extends RuntimeException {

    /**
     * Constructs a new {@code InvalidRoleException} with a descriptive message.
     *
     * @param message the detail message explaining why the role is invalid
     */
    public InvalidRoleException(String message) {
        super(message);
    }
}


