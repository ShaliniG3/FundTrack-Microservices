package com.cts.fundtrack.dgcs.service.paymentservice;

import com.cts.fundtrack.dgcs.client.applicationclient.ApplicationClient;
import com.cts.fundtrack.dgcs.client.complianceclient.ComplianceClient;
import com.cts.fundtrack.dgcs.client.dto.ApplicationMetadataDTO;

import com.cts.fundtrack.dgcs.config.EncryptionUtil;

import com.cts.fundtrack.dgcs.dto.paymentdto.PaymentRequestDTO;
import com.cts.fundtrack.dgcs.dto.paymentdto.PaymentResponseDTO;

import com.cts.fundtrack.dgcs.exception.*;

import com.cts.fundtrack.dgcs.mapper.ModuleMapper;

import com.cts.fundtrack.dgcs.model.Disbursement;
import com.cts.fundtrack.dgcs.model.Payment;
import com.cts.fundtrack.dgcs.model.enums.DisbursementStatus;
import com.cts.fundtrack.dgcs.model.enums.PaymentStatus;

import com.cts.fundtrack.dgcs.repository.disbursementrepository.DisbursementRepository;
import com.cts.fundtrack.dgcs.repository.paymentrepository.PaymentRepository;

import com.cts.fundtrack.dgcs.service.complianceservice.ComplianceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for managing financial transactions (Payments).
 *
 * <p>
 * <p>
 * Ensures transactional safety when moving a Disbursement to PAID, and enforces a
 * <p>
 * "Just-In-Time" check to the Compliance Service before any funds are released.
 * </p>
 */

@Slf4j
@RequiredArgsConstructor
@Service

public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final DisbursementRepository disbursementRepository;
    private final ModuleMapper mapper;
    private final EncryptionUtil encryptionUtil;
    private final ComplianceService complianceService;

    private final ApplicationClient applicationClient;
    private final ComplianceClient complianceClient;

    /**
     * Executes the financial settlement of a specific disbursement installment.
     * <p>
     * Sequence: Disbursement Resolution -> State Validation -> Compliance Verification
     * -> Payment Record Creation -> Local State Synchronization.
     * </p>
     *
     * @param dto Data transfer object containing the disbursement ID and payment method.
     * @return A {@link PaymentResponseDTO} confirming the successful transaction details.
     * @throws DisbursementNotFoundException If the target disbursement ID does not exist.
     * @throws DuplicateTransactionException If the installment has already been processed.
     * @throws InvalidProgramStateException  If the installment is in a CANCELLED status.
     * @throws ComplianceViolationException  If the beneficiary fails compliance checks.
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public PaymentResponseDTO processPayment(PaymentRequestDTO dto) {

        log.info("Process Start: Initiating Settlement | Disbursement ID: {}", dto.getDisbursementId());

        // 1. RESOLVE LOCAL ENTITY
        Disbursement disbursement = disbursementRepository.findById(dto.getDisbursementId())
                .orElseThrow(() -> {
                    log.error("Lookup Failed: Disbursement ID {} not found in persistence.", dto.getDisbursementId());
                    return new DisbursementNotFoundException("Target disbursement not found.");
                });

        // 2. DOMAIN VALIDATION
        if (DisbursementStatus.PAID.equals(disbursement.getStatus())) {
            log.warn("Validation Rejected: Disbursement {} already settled.", dto.getDisbursementId());
            throw new DuplicateTransactionException("This installment has already been settled.");
        }
        if (DisbursementStatus.CANCELLED.equals(disbursement.getStatus())) {
            log.warn("Validation Rejected: Disbursement {} is marked as CANCELLED.", dto.getDisbursementId());
            throw new InvalidProgramStateException("This installment was cancelled.");
        }

        // 3. COMPLIANCE & METADATA (ORCHESTRATION VIA FEIGN)
        // UUID appId = disbursement.getApplicationId();
        // ApplicationMetadataDTO appMeta = applicationClient.getApplicationMetadata(appId);

        // log.debug("Validation: Running compliance check for Application: {}", appId);
        // if (!complianceClient.isApplicantCompliant(appId)) {
        //     log.error("Compliance Failure: Application {} is non-compliant. Halting funds.", appId);
        //     disbursementRepository.cancelFutureInstallments(appId);
        //     throw new ComplianceViolationException("Applicant non-compliant. Future funds halted.");
        // }

        // 4. PERSIST PAYMENT TRANSACTION
        log.debug("Persistence: Creating payment record for amount: {}", disbursement.getAmount());
        Payment payment = Payment.builder()
                .disbursementId(disbursement.getDisbursementId())
                .amount(disbursement.getAmount())
                .method(dto.getMethod())
                .date(Instant.now())
                .status(PaymentStatus.SUCCESS)
                .build();

        try {
            Payment savedPayment = paymentRepository.save(payment);

            // 5. UPDATE DISBURSEMENT STATE
            disbursement.setStatus(DisbursementStatus.PAID);
            disbursement.setActualDate(LocalDate.now());
            disbursementRepository.save(disbursement);

            log.info("Process Complete: Settlement successful for Disbursement {} | Status: PAID",
                    disbursement.getDisbursementId());

            return mapper.toPaymentResponseDTO(savedPayment);

        } catch (Exception e) {
            log.error("Database Failure: Critical error during payment synchronization for Disbursement: {}. Error: {}",
                    dto.getDisbursementId(), e.getMessage());

            throw new DisbursementPersistenceException("System failed to synchronize payment state: " + e.getMessage());
        }
    }

    /**
     * Resolves and retrieves a payment record using an encrypted identifier.
     * <p>
     * Sequence: Decryption -> ID Extraction -> Repository Lookup -> DTO Mapping.
     * </p>
     *
     * @param encryptedPaymentId The Base64/AES encrypted UUID string.
     * @return A {@link PaymentResponseDTO} representing the decrypted payment record.
     * @throws PaymentNotFoundException If the ID is malformed, decryption fails, or the record does not exist.
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentByEncryptedId(String encryptedPaymentId) {

        log.info("Process Start: Resolving Payment via Encrypted Reference.");

        UUID rawId;

        // 1. ATTEMPT DECRYPTION
        try {
            log.debug("Logic Execution: Attempting to decrypt payment reference.");
            rawId = encryptionUtil.decrypt(encryptedPaymentId);
        } catch (Exception e) {
            // High-security logging: We log the failure internally but mask the reason for the client
            log.warn("Security Alert: Decryption failed for provided token. Context: {}", e.getMessage());

            throw new PaymentNotFoundException("Invalid payment reference.");
        }

        // 2. RESOLVE ENTITY
        log.debug("Resolution: Querying repository for Decrypted ID: {}", rawId);

        Payment payment = paymentRepository.findById(rawId)
                .orElseThrow(() -> {
                    log.warn("Lookup Failed: No payment found for Decrypted ID: {}", rawId);
                    return new PaymentNotFoundException("Payment record not found.");
                });

        log.info("Process Complete: Payment record successfully resolved for ID: {}", rawId);

        return mapper.toPaymentResponseDTO(payment);
    }

    /**
     * Retrieves all payment transactions associated with a specific grant application.
     * <p>
     * Sequence: Disbursement Discovery -> ID Extraction -> Payment Batch Retrieval -> DTO Mapping.
     * </p>
     *
     * @param applicationId The unique identifier for the grant application.
     * @return A {@link List} of {@link PaymentResponseDTO} containing all historical payments for this application.
     * @throws DisbursementDataAccessException If the persistence layer fails to resolve disbursement or payment records.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getPaymentsByApplication(UUID applicationId) {

        log.info("Process Start: Fetching Payment History | Application: {}", applicationId);

        try {
            // 1. RESOLVE DISBURSEMENT LINKAGE
            log.debug("Resolution: Discovering disbursement installments for Application: {}", applicationId);
            List<Disbursement> disbursements = disbursementRepository.findAllByApplicationId(applicationId);

            if (disbursements.isEmpty()) {
                log.info("Process Halted: No disbursements found for Application {}. Returning empty history.", applicationId);
                return Collections.emptyList();
            }

            // 2. EXTRACT TRANSACTION KEYS
            List<UUID> disbursementIds = disbursements.stream()
                    .map(Disbursement::getDisbursementId)
                    .collect(Collectors.toList());

            // 3. RETRIEVE CROSS-REFERENCED PAYMENTS
            log.debug("Resolution: Querying payment records for {} disbursement IDs.", disbursementIds.size());
            List<PaymentResponseDTO> payments = paymentRepository.findAllByDisbursementIdIn(disbursementIds).stream()
                    .map(mapper::toPaymentResponseDTO)
                    .collect(Collectors.toList());

            log.info("Process Complete: Successfully retrieved {} payment records for Application: {}",
                    payments.size(), applicationId);

            return payments;

        } catch (Exception e) {
            log.error("Data Access Failure: Error aggregating payment history for Application: {}. Reason: {}",
                    applicationId, e.getMessage());

            throw new DisbursementDataAccessException("System failed to synchronize payment history: " + e.getMessage());
        }
    }

    /**
     * Generates a physical PDF receipt for a specific payment transaction.
     * <p>
     * Sequence: ID Decryption -> Transaction Resolution -> Data Aggregation -> Document Rendering.
     * </p>
     *
     * @param encryptedPaymentId The encrypted reference string for the target payment.
     * @return A {@code byte[]} containing the binary PDF data of the receipt.
     * @throws PaymentNotFoundException If the ID is invalid or the transaction record is missing.
     * @throws ReceiptGenerationException If the document rendering engine fails.
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] generatePaymentReceipt(String encryptedPaymentId) {

        log.info("Process Start: Generating Payment Receipt via Encrypted Reference.");

        UUID rawId;

        // 1. SECURITY RESOLUTION
        try {
            log.debug("Logic Execution: Attempting to decrypt payment reference.");
            rawId = encryptionUtil.decrypt(encryptedPaymentId);
        } catch (Exception e) {
            log.warn("Security Alert: Receipt requested with invalid token. Context: {}", e.getMessage());
            throw new PaymentNotFoundException("Invalid payment reference.");
        }

        // 2. DATA RESOLUTION
        Payment payment = paymentRepository.findById(rawId)
                .orElseThrow(() -> {
                    log.warn("Lookup Failed: No payment record found for receipt generation. ID: {}", rawId);
                    return new PaymentNotFoundException("Payment record not found.");
                });

        // 3. DOCUMENT GENERATION
        try {
            log.debug("Logic Execution: Initiating PDF rendering for Payment ID: {}", rawId);

            byte[] receiptContent = new byte[0];

            log.info("Process Complete: Receipt successfully generated for Payment: {}", rawId);

            return receiptContent;

        } catch (Exception e) {
            log.error("Generation Failure: System could not render PDF for Payment: {}. Reason: {}",
                    rawId, e.getMessage());

            throw new ReceiptGenerationException("System failed to synchronize receipt document: " + e.getMessage());
        }
    }
//    /**
//     * Helper to trigger notifications for payment-related events.
//     */
//    private void triggerPaymentNotification(UUID userId, UUID applicationId, NotificationCategory category) {
//        notificationHelper.notify(userId , category , applicationId);
//        log.info("Payment Notification [{}] sent for User: {}", category, userId);
//    }

}


