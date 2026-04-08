package com.cts.fundtrack.dgcs.exception;

/**
 * Thrown when I/O operations or file processing fails.
 */
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }
}