package com.cts.fundtrack.dgcs.exception;

/**
 * Exception thrown when a user attempts to log out while they are already
 * marked as logged out in the system.
 * <p>
 * This is typically used in authentication workflows where user login status
 * is explicitly tracked, preventing unnecessary or duplicate logout actions.
 */
public class UserAlreadyLoggedOutException extends RuntimeException {

    /**
     * Constructs a new {@code UserAlreadyLoggedOutException} with the specified message.
     *
     * @param message a descriptive message explaining why the logout action is invalid
     */
    public UserAlreadyLoggedOutException(String message) {
        super(message);
    }
}


