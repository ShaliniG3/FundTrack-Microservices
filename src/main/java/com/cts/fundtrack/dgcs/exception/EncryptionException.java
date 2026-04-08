package com.cts.fundtrack.dgcs.exception;

/**
 * Exception thrown when encryption or decryption fails,
 * usually due to tampered data or configuration errors.
 */
public class EncryptionException extends RuntimeException {
    public EncryptionException(String message) {
        super(message);
    }
}