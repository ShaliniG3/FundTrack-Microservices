package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when an applicant attempts to submit a report but has
 * already reached the maximum report-to-disbursement ratio.
 */
public class ReportEligibilityException extends RuntimeException {
    public ReportEligibilityException(String message) {
        super(message);
    }
}