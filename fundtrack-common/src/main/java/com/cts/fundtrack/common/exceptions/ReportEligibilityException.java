package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when an applicant attempts to submit a grant progress report but has already
 * reached the maximum permitted report-to-disbursement ratio for their application.
 *
 * <p>The system enforces an upper bound on the number of reports an applicant may submit
 * per disbursement installment. This exception is raised by the Grant Report Service
 * during the submission eligibility check. Mapped to {@code HTTP 403 Forbidden}
 * by the {@link GlobalExceptionHandler}.</p>
 */
public class ReportEligibilityException extends RuntimeException {

    /**
     * Constructs the exception with a message explaining the eligibility violation.
     *
     * @param message human-readable description of why the report submission is not permitted
     */
    public ReportEligibilityException(String message) {
        super(message);
    }
}