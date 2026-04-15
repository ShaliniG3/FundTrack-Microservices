package com.cts.fundtrack.common.exceptions;

/**
 * Exception thrown when a user attempts to log in while already marked as logged in.
 * <p>
 * This is typically used to prevent multiple concurrent sessions or to ensure
 * state consistency when the system tracks user login status explicitly.
 */
public class UserAlreadyLoggedInException extends RuntimeException {

    /**
     * Constructs a new {@code UserAlreadyLoggedInException} with the provided message.
     *
     * @param message a descriptive message explaining why the user is considered already logged in
     */
    public UserAlreadyLoggedInException(String message) {
        super(message);
    }
}

