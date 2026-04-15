package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when I/O operations or file processing fails.
 */
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }
}