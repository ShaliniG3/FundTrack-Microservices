package com.cts.fundtrack.dgcs.exception;

/**
 * Exception thrown when a process or entity fails to meet mandatory regulatory
 * or internal policy requirements.
 * <p>
 * In the context of Fund-Track, this is typically used during:
 * <ul>
 * <li>Document verification (e.g., fraudulent or missing proofs)</li>
 * <li>Budget allocation checks</li>
 * <li>Audit trail validations</li>
 * </ul>
 * This exception signifies a breach of the "Compliance" rules governing
 * grant management.
 * </p>
 */
public class ComplianceViolationException extends RuntimeException {

	/**
	 * Constructs a new ComplianceViolationException with a detailed error message.
	 *
	 * @param message the detail message describing the specific compliance rule
	 * that was violated.
	 */
	public ComplianceViolationException(String message) {
		super(message);
	}
}