package com.cts.fundtrack.dgcs.exception;

/**
 * Exception thrown when an attempt is made to process a financial transaction
 * that has already been recorded or finalized.
 * <p>
 * This serves as a critical concurrency and integrity guard in the Finance module
 * to prevent:
 * <ul>
 * <li>Double-disbursement of grant funds to the same application</li>
 * <li>Redundant payment gateway processing for a single request</li>
 * <li>Duplicate audit entries for a single financial event</li>
 * </ul>
 * </p>
 * <p>
 * In a REST context, this is typically mapped to {@link org.springframework.http.HttpStatus#CONFLICT} (409).
 * </p>
 */
public class DuplicateTransactionException extends RuntimeException {

	/**
	 * Constructs a new DuplicateTransactionException with a detailed error message.
	 *
	 * @param message the detail message describing the nature of the duplicate
	 * (e.g., "Transaction ID TXN-999 already processed for this application").
	 */
	public DuplicateTransactionException(String message) {
		super(message);
	}
}