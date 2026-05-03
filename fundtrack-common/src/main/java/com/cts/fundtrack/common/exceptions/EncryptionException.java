package com.cts.fundtrack.common.exceptions;

/**
 * Exception thrown when an encryption or decryption operation fails, typically due to
 * tampered ciphertext, an invalid or missing key, or an unsupported algorithm configuration.
 *
 * <p>Raised by {@code EncryptionUtil} in the Disbursement Service when AES operations
 * cannot complete successfully. Mapped to {@code HTTP 400 Bad Request} by the
 * {@link GlobalExceptionHandler}.</p>
 */
public class EncryptionException extends RuntimeException {

    /**
     * Constructs the exception with a message describing the cryptographic failure.
     *
     * @param message human-readable description of the encryption or decryption error
     */
    public EncryptionException(String message) {
        super(message);
    }
}