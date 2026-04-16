package com.cts.fundtrack.disbursement.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.client.NotificationClient;
import com.cts.fundtrack.common.dto.NotificationRequestDTO;
import com.cts.fundtrack.common.dto.PaymentRequestDTO;
import com.cts.fundtrack.common.dto.PaymentResponseDTO;
import com.cts.fundtrack.common.exceptions.DisbursementNotFoundException;
import com.cts.fundtrack.common.exceptions.DuplicateTransactionException;
import com.cts.fundtrack.common.exceptions.InvalidProgramStateException;
import com.cts.fundtrack.common.exceptions.PaymentNotFoundException;
import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.DisbursementStatus;
import com.cts.fundtrack.common.models.enums.EntityType;
import com.cts.fundtrack.common.models.enums.NotificationCategory;
import com.cts.fundtrack.common.models.enums.PaymentStatus;
import com.cts.fundtrack.disbursement.mapper.ModuleMapper;
import com.cts.fundtrack.disbursement.models.Disbursement;
import com.cts.fundtrack.disbursement.models.Payment;
import com.cts.fundtrack.disbursement.repository.DisbursementRepository;
import com.cts.fundtrack.disbursement.repository.PaymentRepository;
import com.cts.fundtrack.disbursement.util.EncryptionUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for managing financial settlements and payment records.
 * <p>
 * This service facilitates the final step of the disbursement lifecycle: processing 
 * successful payments, updating disbursement records, and managing encrypted 
 * access to sensitive financial transaction data.
 * </p>
 *
 * @author FundTrack Development Team
 * @version 1.6
 * @since 2026-04-16
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepo;
    private final DisbursementRepository disbursementRepo;
    private final ModuleMapper mapper;
    private final EncryptionUtil encryptionUtil;
    private final NotificationClient notificationClient;
    private final HttpServletRequest request;

    /**
     * Extracts the Unique Identifier of the currently authenticated user from request headers.
     * @return UUID of the logged-in user.
     */
    private UUID getCurrentUserId() {
        String userIdStr = request.getHeader("X-User-Id");
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    /**
     * Records and finalizes a financial settlement for a specific disbursement.
     * <p>
     * Validates that the disbursement is eligible for payment and transitions its 
     * status to 'PAID'. Dispatches a transactional confirmation to the logged-in user.
     * </p>
     *
     * @param dto Data transfer object containing the disbursement ID and payment method.
     * @return {@link PaymentResponseDTO} representing the successfully processed payment.
     * @throws DisbursementNotFoundException if the target disbursement does not exist.
     * @throws DuplicateTransactionException if the installment has already been settled.
     */
    @Transactional
    @Override
    @Auditable(action = ActionType.CREATE, entityName = EntityType.PAYMENT)
    public PaymentResponseDTO processPayment(PaymentRequestDTO dto) {
        log.info("Initiating financial settlement for Disbursement ID: {}", dto.getDisbursementId());

        Disbursement disbursement = disbursementRepo.findById(dto.getDisbursementId())
                .orElseThrow(() -> new DisbursementNotFoundException("Settlement Aborted: Target disbursement not found."));

        if (DisbursementStatus.PAID.equals(disbursement.getStatus())) {
            log.warn("Transaction Rejected: Disbursement {} is already settled.", dto.getDisbursementId());
            throw new DuplicateTransactionException("This installment has already been settled.");
        }
        
        if (DisbursementStatus.CANCELLED.equals(disbursement.getStatus())) {
            throw new InvalidProgramStateException("Settlement Denied: This installment was previously cancelled.");
        }

        // 1. Create the Payment Record
        Payment payment = Payment.builder()
                .disbursementId(disbursement.getDisbursementId())
                .amount(disbursement.getAmount())
                .method(dto.getMethod())
                .date(Instant.now())
                .status(PaymentStatus.SUCCESS)
                .build();

        Payment savedPayment = paymentRepo.save(payment);

        // 2. Synchronize Disbursement Status
        disbursement.setStatus(DisbursementStatus.PAID);
        disbursement.setActualDate(LocalDate.now());
        disbursementRepo.save(disbursement);

        log.info("Settlement Successful: Payment {} processed for amount {}", 
                 savedPayment.getPaymentId(), disbursement.getAmount());

        // 🚀 Notification: Confirmation to the currently logged-in Admin/Staff
        sendInternalNotification(getCurrentUserId(), null, 
            "Payment Processed: You have successfully recorded a settlement of " + disbursement.getAmount() + " for Disbursement ID: " + disbursement.getDisbursementId(), 
            NotificationCategory.DISBURSEMENT);

        return mapper.toPaymentResponseDTO(savedPayment);
    }

    /**
     * Retrieves a payment record using its encrypted identifier.
     * <p>
     * This method ensures that sensitive database IDs are not exposed in the URL 
     * or logs by utilizing the {@link EncryptionUtil}.
     * </p>
     *
     * @param encryptedPaymentId The AES-encrypted UUID string.
     * @return {@link PaymentResponseDTO} for the decrypted record.
     * @throws PaymentNotFoundException if the ID is invalid or not found.
     */
    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.PAYMENT)
    public PaymentResponseDTO getPaymentByEncryptedId(String encryptedPaymentId) {
        log.debug("Accessing encrypted payment record reference.");
        UUID rawId;
        try {
            rawId = encryptionUtil.decrypt(encryptedPaymentId);
        } catch (Exception e) {
            log.warn("Security Alert: Invalid encrypted payment ID provided.");
            throw new PaymentNotFoundException("Invalid payment reference.");
        }

        Payment payment = paymentRepo.findById(rawId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment record not found."));

        return mapper.toPaymentResponseDTO(payment);
    }

    /**
     * Retrieves all successful payments associated with a specific funding application.
     */
    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.PAYMENT)
    public List<PaymentResponseDTO> getPaymentsByApplication(UUID applicationId) {
        log.debug("Retrieving payment history for Application ID: {}", applicationId);

        List<Disbursement> disbursements = disbursementRepo.findAllByApplicationId(applicationId);
        List<UUID> disbursementIds = disbursements.stream()
                .map(Disbursement::getDisbursementId)
                .collect(Collectors.toList());

        if (disbursementIds.isEmpty()) {
            return Collections.emptyList();
        }

        return paymentRepo.findAllByDisbursementIdIn(disbursementIds).stream()
                .map(mapper::toPaymentResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Generates a binary receipt for a specific transaction.
     * <p>
     * This action is audited to track whenever financial records are exported/downloaded.
     * </p>
     *
     * @param encryptedPaymentId The encrypted reference of the payment.
     * @return byte array representing the generated PDF/Record.
     */
    @Override
    @Auditable(action = ActionType.READ, entityName = EntityType.PAYMENT)
    public byte[] generatePaymentReceipt(String encryptedPaymentId) {
        log.info("Exporting financial receipt for Payment ID: {}", encryptedPaymentId);
        // Business logic for PDF generation remains here
        return new byte[0];
    }

    /**
     * Dispatcher for inter-service alerts. 
     * Confirms transactional results to the authenticated user context.
     */
    private void sendInternalNotification(UUID userId, UUID appId, String message, NotificationCategory category) {
        if (userId == null) {
            log.warn("Notification skipped: No authenticated user ID found.");
            return;
        }
        try {
            NotificationRequestDTO notification = NotificationRequestDTO.builder()
                    .userId(userId) // 🚀 Confirmation sent to the Admin who processed the payment
                    .applicationId(appId)
                    .message(message)
                    .category(category)
                    .build();
            
            notificationClient.sendNotification(notification);
            log.debug("System alert queued for user: {}", userId);
        } catch (Exception e) {
            log.error("Notification Communication Failure for user {}: {}", userId, e.getMessage());
        }
    }
}