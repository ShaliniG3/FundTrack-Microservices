package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when a file I/O or storage operation fails during document upload or retrieval.
 *
 * <p>Raised by the {@code FileStorageService} in the Disbursement Service when reading,
 * writing, or streaming grant report supporting documents encounters an unrecoverable error.
 * Common causes include insufficient disk space, permission errors, or corrupt upload streams.
 * Mapped to {@code HTTP 500 Internal Server Error} by the {@link GlobalExceptionHandler}.</p>
 */
public class FileStorageException extends RuntimeException {

    /**
     * Constructs the exception with a message describing the storage failure.
     *
     * @param message human-readable description of the file I/O or storage error
     */
    public FileStorageException(String message) {
        super(message);
    }
}