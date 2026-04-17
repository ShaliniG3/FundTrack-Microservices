package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when an uploaded document has a MIME type or file extension that is not
 * accepted by the system's document handling policy.
 *
 * <p>The FundTrack platform only accepts specific document formats (e.g., PDF, JPEG, PNG)
 * for grant application supporting evidence. This exception is raised by the Document or
 * File Storage module when a file of a disallowed type is submitted.
 * Mapped to {@code HTTP 400 Bad Request} by the {@link GlobalExceptionHandler}.</p>
 */
public class UnsupportedDocumentTypeException extends RuntimeException {

    /**
     * Constructs the exception with a message identifying the unsupported document type.
     *
     * @param message human-readable description indicating which document type was rejected
     *                and what types are permitted
     */
    public UnsupportedDocumentTypeException(String message) {
        super(message);
    }
}