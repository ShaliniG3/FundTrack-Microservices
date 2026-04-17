package com.cts.fundtrack.disbursement.controller; // Aligned with microservice package

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // ACTIVATED
import org.springframework.web.bind.annotation.*;

import com.cts.fundtrack.common.dto.PaymentRequestDTO;
import com.cts.fundtrack.common.dto.PaymentResponseDTO;
import com.cts.fundtrack.disbursement.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller exposing payment processing and retrieval endpoints for the FundTrack platform.
 * <p>
 * This controller manages the execution layer of the disbursement lifecycle — after an
 * installment schedule is created, Finance Officers use these endpoints to record the
 * actual movement of funds. It also provides secure, encrypted-ID-based access to
 * individual payment records and PDF receipts for applicants and officers.
 * </p>
 * <p>
 * All endpoints are served under {@code /api/v1/payments}. Payment IDs exposed to
 * external callers are AES-encrypted to prevent ID enumeration attacks.
 * </p>
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment Management", description = "Endpoints for processing transactions and generating receipts.")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Records and executes a disbursement payment transaction.
     * <p>
     * Validates that the target disbursement exists, is not already settled ({@code PAID}),
     * and is not cancelled. On success, a {@code Payment} record is persisted, the parent
     * disbursement is marked {@code PAID}, and a confirmation notification is dispatched
     * to the processing officer. Returns {@code 201 Created}. Restricted to Finance
     * Officers and Admins.
     * </p>
     *
     * @param dto the {@link PaymentRequestDTO} containing the disbursement ID and the
     *            payment method (e.g., BANK_TRANSFER, CHEQUE)
     * @return a {@link ResponseEntity} with HTTP 201 and a {@link PaymentResponseDTO}
     *         containing the new payment's encrypted ID, amount, and status
     */
    @PostMapping("/process")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Process Disbursement Payment")
    public ResponseEntity<PaymentResponseDTO> processPayment(@Valid @RequestBody PaymentRequestDTO dto) {
        log.info("Processing payment for Disbursement ID: {}", dto.getDisbursementId());
        PaymentResponseDTO res = paymentService.processPayment(dto);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    /**
     * Retrieves a single payment record using its AES-encrypted identifier.
     * <p>
     * The encrypted ID is decrypted server-side before the database lookup, preventing
     * raw UUID exposure in URLs. Accessible by Finance Officers, Compliance Officers,
     * Admins, or the Applicant who owns the payment (verified via
     * {@code @securityService.isPaymentOwner}).
     * </p>
     *
     * @param encryptedId the AES/Base64-encoded payment identifier as returned in
     *                    {@link com.cts.fundtrack.common.dto.PaymentResponseDTO#getEncryptedPaymentId()}
     * @return a {@link ResponseEntity} containing the {@link PaymentResponseDTO} for
     *         the matched payment record
     */
    @GetMapping("/{encryptedId}")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'ADMIN', 'COMPLIANCE_OFFICER') or " +
                  "(hasRole('APPLICANT') and @securityService.isPaymentOwner(#encryptedId))")
    @Operation(summary = "Get Payment by Secure ID")
    public ResponseEntity<PaymentResponseDTO> getPaymentByEncryptedId(@PathVariable String encryptedId) {
        log.debug("Fetching payment for encrypted ID: {}", encryptedId);
        PaymentResponseDTO res = paymentService.getPaymentByEncryptedId(encryptedId);
        return ResponseEntity.ok(res);
    }

    /**
     * Retrieves the complete payment transaction history for a grant application.
     * <p>
     * Resolves all disbursement installments for the given application and returns
     * every payment record linked to those installments. Returns an empty list if no
     * payments have been processed yet. Accessible by Finance Officers, Compliance
     * Officers, Admins, or the owning Applicant.
     * </p>
     *
     * @param applicationId the UUID of the grant application whose payment history is requested
     * @return a {@link ResponseEntity} containing a list of {@link PaymentResponseDTO}
     *         objects; returns an empty list if no payments exist for the application
     */
    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'ADMIN', 'COMPLIANCE_OFFICER') or " +
                  "(hasRole('APPLICANT') and @securityService.isApplicationOwner(#applicationId))")
    @Operation(summary = "Get Application Payment History")
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentsByApplication(@PathVariable UUID applicationId) {
        log.info("Fetching payment history for Application ID: {}", applicationId);
        List<PaymentResponseDTO> res = paymentService.getPaymentsByApplication(applicationId);
        return ResponseEntity.ok(res);
    }

    /**
     * Generates and streams a PDF payment receipt for a given payment.
     * <p>
     * The encrypted payment ID is resolved to the underlying payment record, and a
     * PDF receipt is generated containing transaction details. The response uses
     * {@code application/pdf} content type so browsers treat it as a file download.
     * Accessible by Finance Officers, Admins, or the Applicant who owns the payment.
     * </p>
     *
     * @param encryptedId the AES/Base64-encoded payment identifier
     * @return a {@link ResponseEntity} containing the raw PDF bytes with
     *         {@code Content-Type: application/pdf}
     */
    @GetMapping(value = "/{encryptedId}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'ADMIN') or " +
                  "(hasRole('APPLICANT') and @securityService.isPaymentOwner(#encryptedId))")
    @Operation(summary = "Download Payment Receipt")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable String encryptedId) {
        log.info("Generating receipt for Payment ID: {}", encryptedId);
        byte[] receipt = paymentService.generatePaymentReceipt(encryptedId);
        return ResponseEntity.ok(receipt);
    }
}