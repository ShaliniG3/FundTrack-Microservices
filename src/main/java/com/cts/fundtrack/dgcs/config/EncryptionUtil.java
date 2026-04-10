package com.cts.fundtrack.dgcs.config;

import com.cts.fundtrack.dgcs.exception.EncryptionException;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

/**
 * Utility component for encrypting and decrypting UUID identifiers.
 * <p>
 * This class provides a layer of security by obfuscating internal database IDs
 * into URL-safe Base64 encoded strings, preventing ID enumeration attacks.
 * It uses the AES-128 encryption algorithm.
 * </p>
 *
 * @author FundTrack Team
 * @version 1.0
 */
@Slf4j
@Component
public class EncryptionUtil {

    /**
     * The secret key used for AES encryption.
     * @deprecated In production, move this to an environment variable or secret vault.
     */
    private static final String SECRET_KEY = "FundTrack_SecretKey";

    /** The symmetric encryption algorithm used by this utility. */
    private static final String ALGORITHM = "AES";

    /**
     * Generates a 128-bit {@link SecretKeySpec} based on the defined {@code SECRET_KEY}.
     * <p>
     * It hashes the key string using SHA-1 and truncates it to 16 bytes to ensure
     * compatibility with the AES-128 algorithm regardless of the input key length.
     * </p>
     *
     * @return A valid SecretKeySpec for AES encryption.
     * @throws Exception if the hashing algorithm is not available.
     */
    private SecretKeySpec getValidKey() throws Exception {
        byte[] key = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16);
        return new SecretKeySpec(key, ALGORITHM);
    }

    /**
     * Encrypts a standard {@link UUID} into a URL-safe Base64 string.
     *
     * @param id The raw UUID to be encrypted.
     * @return A URL-safe encrypted String, or {@code null} if the input is null.
     * @throws EncryptionException if an error occurs during the encryption process.
     */
    @Schema(description = "Encrypts a UUID to a secure string format", example = "S0VZX0VOQ1JZUFRFRA==")
    public String encrypt(UUID id) {
        if (id == null) return null;

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getValidKey());

            byte[] encryptedBytes = cipher.doFinal(id.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Encryption failed for ID {}: {}", id, e.getMessage());
            throw new EncryptionException("Internal security error during ID encryption.");
        }
    }

    /**
     * Decrypts a previously encrypted string back into a {@link UUID}.
     *
     * @param encryptedId The Base64 encoded encrypted string.
     * @return The original {@link UUID}, or {@code null} if the input is empty/null.
     * @throws EncryptionException if the string is tampered with or the decryption fails.
     */
    @Schema(description = "Decrypts a secure string back into a UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    public UUID decrypt(String encryptedId) {
        if (encryptedId == null || encryptedId.isEmpty()) return null;

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getValidKey());

            byte[] decodedBytes = Base64.getUrlDecoder().decode(encryptedId);
            String decodedString = new String(cipher.doFinal(decodedBytes), StandardCharsets.UTF_8);
            return UUID.fromString(decodedString);
        } catch (Exception e) {
            log.error("Decryption failed for input {}: {}", encryptedId, e.getMessage());
            throw new EncryptionException("The provided identifier is invalid or has been tampered with.");
        }
    }
}