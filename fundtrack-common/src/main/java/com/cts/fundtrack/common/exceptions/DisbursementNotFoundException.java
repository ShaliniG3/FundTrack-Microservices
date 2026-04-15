package com.cts.fundtrack.common.exceptions;

/**
 * Exception thrown when a specific financial disbursement record cannot be found.
 * <p>
 * This is typically used in the Finance or Payment modules of the Fund-Track system
 * when:
 * <ul>
 * <li>Searching for a payout by its unique Transaction ID</li>
 * <li>Updating the status of a scheduled fund transfer</li>
 * <li>Generating receipts for a specific grant release</li>
 * </ul>
 * It indicates that the requested disbursement entity does not exist in the
 * persistence layer.
 * </p>
 */
public class DisbursementNotFoundException extends RuntimeException {

	/**
	 * Constructs a new DisbursementNotFoundException with a detailed error message.
	 *
	 * @param message the detail message describing the missing disbursement
	 * (e.g., "Disbursement not found for Transaction ID: TXN-123").
	 */
	public DisbursementNotFoundException(String message) {
		super(message);
	}
}