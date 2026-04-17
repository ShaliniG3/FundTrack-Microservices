package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when a grant progress report submission is attempted outside the permitted
 * 90-day temporal reporting window.
 *
 * <p>The FundTrack platform enforces a strict reporting calendar: reports may only be
 * submitted within 90 days of the relevant disbursement installment date. This exception
 * is raised by {@code ReportingWindowValidator} when the submission timestamp falls
 * outside that window. Mapped to {@code HTTP 403 Forbidden}
 * by the {@link GlobalExceptionHandler}.</p>
 */
public class ReportingWindowException extends RuntimeException {

    /**
     * Constructs the exception with a message explaining the reporting window violation.
     *
     * @param message human-readable description of the temporal constraint that was violated
     */
    public ReportingWindowException(String message) {
        super(message);
    }
}