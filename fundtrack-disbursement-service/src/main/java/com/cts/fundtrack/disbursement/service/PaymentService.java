package com.cts.fundtrack.disbursement.service;

import java.util.List;
import java.util.UUID;

import com.cts.fundtrack.common.dto.PaymentRequestDTO;
import com.cts.fundtrack.common.dto.PaymentResponseDTO;

/**
 * Service interface for processing and managing actual grant fund transactions.
 * <p>
 * While the {@link DisbursementService} manages the "plan" (installment schedules),
 * this service manages the "execution" — recording specific payment events, enforcing
 * transaction idempotency, handling payment ID security via AES encryption, and
 * providing payment history for reconciliation and receipt generation.
 * </p>
 */
public interface PaymentService {

    /**
     * Executes and records a disbursement payment transaction.
     * <p>
     * Validates that the target disbursement is in a payable state, creates a
     * {@code Payment} record, and transitions the disbursement to {@code PAID}.
     * Throws exceptions for duplicate or cancelled disbursements.
     * </p>
     *
     * @param dto the {@link PaymentRequestDTO} containing the disbursement ID and payment method
     * @return a {@link PaymentResponseDTO} with the new payment's encrypted ID, amount, and status
     * @throws com.cts.fundtrack.common.exceptions.DisbursementNotFoundException if the disbursement does not exist
     * @throws com.cts.fundtrack.common.exceptions.DuplicateTransactionException if the disbursement is already paid
     * @throws com.cts.fundtrack.common.exceptions.InvalidProgramStateException  if the disbursement is cancelled
     */
    PaymentResponseDTO processPayment(PaymentRequestDTO dto);

    /**
     * Retrieves a payment record by its AES-encrypted identifier.
     * <p>
     * The encrypted ID is decrypted server-side before the database lookup, preventing
     * raw UUID exposure in client-facing URLs.
     * </p>
     *
     * @param encryptedPaymentId the AES/Base64-encoded payment identifier
     * @return the {@link PaymentResponseDTO} for the matched payment record
     * @throws com.cts.fundtrack.common.exceptions.PaymentNotFoundException if the ID is invalid or no record is found
     */
    PaymentResponseDTO getPaymentByEncryptedId(String encryptedPaymentId);

    /**
     * Retrieves all payment transactions associated with a grant application.
     * <p>
     * Resolves all disbursement installments for the application and returns every
     * payment linked to those installments, providing a full financial history.
     * </p>
     *
     * @param applicationId the UUID of the grant application to query
     * @return a list of {@link PaymentResponseDTO} objects; empty if no payments exist
     */
    List<PaymentResponseDTO> getPaymentsByApplication(UUID applicationId);

    /**
     * Generates a PDF payment receipt for a given payment.
     * <p>
     * The encrypted payment ID is resolved to the underlying record and a PDF
     * document is produced containing transaction details for record-keeping.
     * </p>
     *
     * @param encryptedPaymentId the AES/Base64-encoded payment identifier
     * @return the raw PDF content as a byte array
     * @throws com.cts.fundtrack.common.exceptions.PaymentNotFoundException if the ID is invalid or no record is found
     */
    byte[] generatePaymentReceipt(String encryptedPaymentId);
}
