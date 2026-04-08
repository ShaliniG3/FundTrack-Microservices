package com.cts.fundtrack.dgcs.exception;

/**
 * Exception thrown when an action is attempted on a Program that is in
 * an incorrect status (e.g., trying to apply to a 'CLOSED' program).
 * * It ensures that the Program lifecycle rules are followed.
 */
public class InvalidProgramStateException extends RuntimeException {

	/**
	 * Constructs the exception with a specific error message.
	 *
	 * @param message description of the invalid state or action.
	 */
	public InvalidProgramStateException(String message) {
		super(message);
	}
}