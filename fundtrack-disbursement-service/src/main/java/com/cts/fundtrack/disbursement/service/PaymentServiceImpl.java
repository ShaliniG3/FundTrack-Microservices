package com.cts.fundtrack.disbursement.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.common.aspect.Auditable; // 👈 IMPORT YOUR COMMON ANNOTATION
import com.cts.fundtrack.common.dto.PaymentRequestDTO;
import com.cts.fundtrack.common.dto.PaymentResponseDTO;
import com.cts.fundtrack.common.exceptions.*;
import com.cts.fundtrack.common.models.enums.ActionType; // 👈 IMPORT ENUMS
import com.cts.fundtrack.common.models.enums.EntityType;
import com.cts.fundtrack.common.models.enums.DisbursementStatus;
import com.cts.fundtrack.common.models.enums.PaymentStatus;
import com.cts.fundtrack.disbursement.client.ApplicationClient;
import com.cts.fundtrack.disbursement.client.ComplianceClient;
import com.cts.fundtrack.disbursement.mapper.ModuleMapper;
import com.cts.fundtrack.disbursement.models.Disbursement;
import com.cts.fundtrack.disbursement.models.Payment;
import com.cts.fundtrack.disbursement.repository.DisbursementRepository;
import com.cts.fundtrack.disbursement.repository.PaymentRepository;
import com.cts.fundtrack.disbursement.util.EncryptionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for managing financial transactions.
 * Fully audited to ensure every dollar moved has a traceable user and timestamp.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepo;
    private final DisbursementRepository disbursementRepo;
    private final ModuleMapper mapper;
    private final EncryptionUtil encryptionUtil;
    private final ComplianceService complianceService;
    private final ApplicationClient applicationClient;
    private final ComplianceClient complianceClient;

    /**
     * Records a successful financial settlement.
     */
    @Transactional
    @Override
    @Auditable(action = ActionType.CREATE, entityName = EntityType.PAYMENT) // 👈 AUDIT ENABLED
    public PaymentResponseDTO processPayment(PaymentRequestDTO dto) {
        log.info("Initiating settlement for Disbursement ID: {}", dto.getDisbursementId());

        Disbursement disbursement = disbursementRepo.findById(dto.getDisbursementId())
                .orElseThrow(() -> new DisbursementNotFoundException("Target disbursement not found."));

        if (DisbursementStatus.PAID.equals(disbursement.getStatus())) {
            throw new DuplicateTransactionException("This installment has already been settled.");
        }
        if (DisbursementStatus.CANCELLED.equals(disbursement.getStatus())) {
            throw new InvalidProgramStateException("This installment was cancelled.");
        }

        // Create the Payment Record
        Payment payment = Payment.builder()
                .disbursementId(disbursement.getDisbursementId())
                .amount(disbursement.getAmount())
                .method(dto.getMethod())
                .date(Instant.now())
                .status(PaymentStatus.SUCCESS)
                .build();

        Payment savedPayment = paymentRepo.save(payment);

        // Update Disbursement Status Locally
        disbursement.setStatus(DisbursementStatus.PAID);
        disbursement.setActualDate(LocalDate.now());
        disbursementRepo.save(disbursement);

        return mapper.toPaymentResponseDTO(savedPayment);
    }

    /**
     * Audits access to sensitive encrypted payment data.
     */
    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.PAYMENT) // 👈 AUDIT ENABLED
    public PaymentResponseDTO getPaymentByEncryptedId(String encryptedPaymentId) {
        UUID rawId;
        try {
            rawId = encryptionUtil.decrypt(encryptedPaymentId);
        } catch (Exception e) {
            log.warn("Invalid encrypted ID provided: {}", encryptedPaymentId);
            throw new PaymentNotFoundException("Invalid payment reference.");
        }

        Payment payment = paymentRepo.findById(rawId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment record not found."));

        return mapper.toPaymentResponseDTO(payment);
    }

    /**
     * Audits retrieval of a list of payments for an application.
     */
    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.PAYMENT) // 👈 AUDIT ENABLED
    public List<PaymentResponseDTO> getPaymentsByApplication(UUID applicationId) {
        log.debug("Fetching payments for Application: {}", applicationId);

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
     * Audits receipt generation (Downloading financial records).
     */
    @Override
    @Auditable(action = ActionType.READ, entityName = EntityType.PAYMENT) // 👈 AUDIT ENABLED
    public byte[] generatePaymentReceipt(String encryptedPaymentId) {
        // Your receipt generation logic here
        return new byte[0];
    }
}