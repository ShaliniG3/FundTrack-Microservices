package com.cts.fundtrack.dgcs.exception;

/**
 * Exception thrown when a specific payment record or transaction
 * cannot be found in the system.
 */
public class PaymentNotFoundException extends RuntimeException {

	/**
	 * Constructs the exception with a detailed message.
	 * * @param message description of the missing payment (e.g., "Payment ID not found").
	 */
	public PaymentNotFoundException(String message) {
		super(message);
	}
}