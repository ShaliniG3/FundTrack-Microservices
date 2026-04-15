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

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment Management", description = "Endpoints for processing transactions and generating receipts.")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Executes a payment. Restricted to Finance Officers and Admins.
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
     * Get payment by secure ID. Accessible by Officers or the Owner (Applicant).
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
     * Get payment history for an application. Accessible by Officers or the Application Owner.
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
     * Download PDF receipt. Restricted to Finance/Admin or the Payment Owner.
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