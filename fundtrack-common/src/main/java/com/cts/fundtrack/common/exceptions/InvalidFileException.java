package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when an uploaded file fails validation checks such as format, size,
 * MIME type, or content integrity verification.
 *
 * <p>Raised by the Document or File Storage modules when a submitted file does not
 * meet the system's acceptance criteria. Common triggers include unsupported file
 * types, files exceeding the size limit, or empty/corrupt uploads.
 * Mapped to {@code HTTP 400 Bad Request} by the {@link GlobalExceptionHandler}.</p>
 */
public class InvalidFileException extends RuntimeException {

    /**
     * Constructs the exception with a descriptive message explaining why the file
     * was rejected.
     *
     * @param message human-readable description of the file validation failure
     */
    public InvalidFileException(String message) {
        super(message);
    }
}