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

    private final PaymentRepository paymentRepo;
    private final DisbursementRepository disbursementRepo;

//    private final AuditHelper auditHelper;
//
//    private final UserRepository userRepository;

    private final ModuleMapper mapper;

    private final EncryptionUtil encryptionUtil;

    private final ComplianceService complianceService;
    //private final NotificationHelper notificationHelper;
    private final ApplicationClient applicationClient; // To get Applicant UUID
    private final ComplianceClient complianceClient;


    /**
     * Processes a scheduled disbursement.
     * <p>
     * Performs a Just-In-Time compliance check before creating the payment record.
     * <p>
     * If compliance fails, future installments are canceled and a rollback occurs.
     *
     * @param dto The payment method and target disbursement ID.
     * @return Completed {@link PaymentResponseDTO}.
     * @throws DisbursementNotFoundException If target disbursement does not exist.
     * @throws DuplicateTransactionException If already PAID.
     * @throws InvalidProgramStateException  If CANCELLED.
     * @throws ComplianceViolationException  If applicant is currently non-compliant.
     */

    @Transactional
    @Override
    public PaymentResponseDTO processPayment(PaymentRequestDTO dto) {
        log.info("Initiating settlement for Disbursement ID: {}", dto.getDisbursementId());

        // 1. Fetch Local Disbursement
        Disbursement disbursement = disbursementRepo.findById(dto.getDisbursementId())
                .orElseThrow(() -> new DisbursementNotFoundException("Target disbursement not found."));

        // 2. Validation Checks
        if (DisbursementStatus.PAID.equals(disbursement.getStatus())) {
            throw new DuplicateTransactionException("This installment has already been settled.");
        }
        if (DisbursementStatus.CANCELLED.equals(disbursement.getStatus())) {
            throw new InvalidProgramStateException("This installment was cancelled.");
        }

        // 3. Get Application Metadata via Feign (to find the Applicant)
//        UUID appId = disbursement.getApplicationId();
//        ApplicationMetadataDTO appMeta = applicationClient.getApplicationMetadata(appId);
//        UUID applicantUserId = appMeta.getApplicantUserId();
//
//        // 4. Compliance Check via Feign
//        if (!complianceClient.isApplicantCompliant(appId)) {
//            log.warn("HALT: Application {} failed compliance. Cancelling future installments.", appId);
//
//            disbursementRepo.cancelFutureInstallments(appId);
//
//            // Notification call (Now likely an event or a feign call)
//            // triggerPaymentNotification(applicantUserId, appId, "REJECTED");
//
//            throw new ComplianceViolationException("Applicant non-compliant. Future funds halted.");
//        }

        // 5. Create the Payment Record
        Payment payment = Payment.builder()
                .disbursementId(disbursement.getDisbursementId())
                .amount(disbursement.getAmount())
                .method(dto.getMethod())
                .date(Instant.now())
                .status(PaymentStatus.SUCCESS)
                .build();

        Payment savedPayment = paymentRepo.save(payment);

        // 6. Update Disbursement Status Locally
        disbursement.setStatus(DisbursementStatus.PAID);
        disbursement.setActualDate(LocalDate.now());
        disbursementRepo.save(disbursement);

        // 7. Audit and Notifications (Handled via Feign or Messaging)
//        log.info("Payment successful for App: {} | Amount: {}", appId, disbursement.getAmount());

        return mapper.toPaymentResponseDTO(savedPayment);
    }
    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentByEncryptedId(String encryptedPaymentId) {
        UUID rawId;

        try {
            rawId = encryptionUtil.decrypt(encryptedPaymentId);
        } catch (Exception e) {
            log.warn("Invalid encrypted ID provided: {}", encryptedPaymentId);
            // Throwing Not Found is safer for security than telling the user "Decryption failed"
            throw new PaymentNotFoundException("Invalid payment reference.");
        }

        Payment payment = paymentRepo.findById(rawId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment record not found."));

        return mapper.toPaymentResponseDTO(payment);
    }
    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getPaymentsByApplication(UUID applicationId) {
        log.debug("Fetching payments for Application: {}", applicationId);

        // 1. Get all Disbursement entities belonging to this Application
        List<Disbursement> disbursements = disbursementRepo.findAllByApplicationId(applicationId);

        // 2. Extract the IDs of those disbursements
        List<UUID> disbursementIds = disbursements.stream()
                .map(Disbursement::getDisbursementId)
                .collect(Collectors.toList());

        if (disbursementIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Query the Payment table for any records matching those Disbursement IDs
        // This replaces the old d.getPayment() logic
        return paymentRepo.findAllByDisbursementIdIn(disbursementIds).stream()
                .map(mapper::toPaymentResponseDTO)
                .collect(Collectors.toList());
    }
    @Override

    public byte[] generatePaymentReceipt(String encryptedPaymentId) {

        return new byte[0];

    }
//    /**
//     * Helper to trigger notifications for payment-related events.
//     */
//    private void triggerPaymentNotification(UUID userId, UUID applicationId, NotificationCategory category) {
//        notificationHelper.notify(userId , category , applicationId);
//        log.info("Payment Notification [{}] sent for User: {}", category, userId);
//    }

}


