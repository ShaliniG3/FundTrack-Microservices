package com.cts.fundtrack.dgcs.service.paymentservice;

import com.cts.fundtrack.dgcs.dto.paymentdto.PaymentRequestDTO;
import com.cts.fundtrack.dgcs.dto.paymentdto.PaymentResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for processing and managing actual grant fund transactions.
 * <p>
 * This service acts as the financial execution layer of the DGCS system. While the
 * Disbursement service manages the scheduled plan, this interface handles the
 * realization of payments, transaction security via ID obfuscation, and the
 * generation of legally binding fiscal receipts.
 * </p>
 */
public interface PaymentService {

    /**
     * Finalizes and records a fund transfer event for a specific grant installment.
     * <p>
     * This method marks the transition from a 'SCHEDULED' disbursement to a 'PAID'
     * transaction. It captures transaction references from external banking gateways
     * and updates the application's remaining balance.
     * </p>
     *
     * @param dto The {@link PaymentRequestDTO} containing transaction details and banking references.
     * @return A {@link PaymentResponseDTO} confirming the persisted payment record.
     */
    PaymentResponseDTO processPayment(PaymentRequestDTO dto);

    /**
     * Retrieves a specific payment record using a secure, encrypted identifier.
     * <p>
     * Utilizes the {@code EncryptionUtil} to decrypt the provided token before
     * querying the database. This prevents ID enumeration attacks in public-facing
     * receipt URLs or transaction lookups.
     * </p>
     *
     * @param encryptedPaymentId The URL-safe Base64 encoded string representing the Payment ID.
     * @return The detailed {@link PaymentResponseDTO} associated with the decrypted ID.
     */
    PaymentResponseDTO getPaymentByEncryptedId(String encryptedPaymentId);

    /**
     * Retrieves the comprehensive transaction history for a single grant application.
     * <p>
     * Provides an immutable ledger of all funds received by the applicant, sorted
     * chronologically to facilitate financial reconciliation and compliance audits.
     * </p>
     *
     * @param applicationId The internal {@link UUID} of the grant application.
     * @return A list of {@link PaymentResponseDTO} objects representing the applicant's ledger.
     */
    List<PaymentResponseDTO> getPaymentsByApplication(UUID applicationId);

    /**
     * Generates a binary PDF document serving as an official proof of payment.
     * <p>
     * This document includes the digital signature of the FundTrack system,
     * transaction timestamps, and the disbursement milestone details. Access is
     * secured via the encrypted identifier.
     * </p>
     *
     * @param encryptedPaymentId The secure token used to identify the target transaction.
     * @return A byte array representing the PDF content of the payment receipt.
     */
    byte[] generatePaymentReceipt(String encryptedPaymentId);
}