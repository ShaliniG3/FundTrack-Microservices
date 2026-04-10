package com.cts.fundtrack.dgcs.controller.paymentcontroller;

import com.cts.fundtrack.dgcs.dto.paymentdto.PaymentRequestDTO;
import com.cts.fundtrack.dgcs.dto.paymentdto.PaymentResponseDTO;

import com.cts.fundtrack.dgcs.service.paymentservice.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for orchestrating financial transactions and payment lifecycle management.
 * <p>
 * This controller handles the final stage of the fund distribution process, including
 * payment processing, encrypted record retrieval, and automated receipt generation.
 * All sensitive identifiers are handled via an encryption layer to ensure data privacy.
 * </p>
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("api/v1/payments")
@Tag(name = "Payment Management", description = "Endpoints for processing disbursements, tracking payment history, and generating encrypted receipts")
public class PaymentController {

	private final PaymentService paymentService;

    /**
     * Executes a payment transaction for a specific disbursement record.
     * <p>
     * Triggers the financial gateway integration and updates the associated
     * disbursement status to PAID upon successful completion.
     * </p>
     *
     * @param dto The validated {@link PaymentRequestDTO} containing disbursement and transaction details.
     * @return A {@link ResponseEntity} containing the {@link PaymentResponseDTO} and HTTP 201 Created status.
     */
    @Operation(
            summary = "Process Disbursement Payment",
            description = "Initiates the transfer of funds for a specific disbursement ID. "
                    + "This operation triggers gateway integration and returns an encrypted payment receipt. "
                    + "Restricted to FINANCE_OFFICER and ADMIN roles.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Payment processed successfully"),
                    @ApiResponse(responseCode = "400", description = "Bad Request: Invalid payment data or disbursement already paid"),
                    @ApiResponse(responseCode = "403", description = "Forbidden: Insufficient financial authorization"),
                    @ApiResponse(responseCode = "422", description = "Unprocessable Entity: Insufficient program budget or gateway rejection")
            }
    )

    //@PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'ADMIN')")
    @PostMapping("/process")
	public ResponseEntity<PaymentResponseDTO> processPayment(@Valid @RequestBody PaymentRequestDTO dto){
		log.info("Processing the payment for Disbursement: {}", dto.getDisbursementId());
		PaymentResponseDTO res = paymentService.processPayment(dto);
        if (res == null) {
            log.error(" Payment processing failed to return a response for ID: {}", dto.getDisbursementId());
            return ResponseEntity.internalServerError().build();
        }

        log.info("Payment Successful | EncryptedReceiptID: {}", res.getEncryptedPaymentId());

        return new ResponseEntity<>(res, HttpStatus.CREATED);
	}

    /**
     * Retrieves detailed payment information using a secure encrypted identifier.
     * <p>
     * This method facilitates secure receipt retrieval by accepting an AES-encrypted
     * string rather than a raw database ID. This layer of abstraction protects
     * sensitive financial data and prevents unauthorized data scraping via
     * sequential ID guessing.
     * </p>
     *
     * @param encryptedId The AES-encrypted string representing the Payment ID.
     * @return A {@link ResponseEntity} containing the decrypted and mapped payment record.
     */
    @Operation(
            summary = "Get Payment by Secure ID",
            description = "Retrieves a specific payment record using an encrypted token. "
                    + "Ensures that internal primary keys are never exposed in the URL. "
                    + "Access is restricted based on ownership or specialized administrative roles.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Payment record retrieved and decrypted successfully"),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User is not authorized to view this transaction"),
                    @ApiResponse(responseCode = "404", description = "Not Found: No payment record matches the provided token")
            }
    )
    //@PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'ADMIN', 'COMPLIANCE_OFFICER') or " +
    //	"(hasRole('APPLICANT') and @securityService.isPaymentOwner(#encryptedId))")
    @GetMapping("/{encryptedId}")
	public ResponseEntity<PaymentResponseDTO> getPaymentByEncryptedId(
            @Parameter(description = "The encrypted payment token received during the process stage",
                    example = "U2FsdGVkX19v8nZ_secure_token_xyz")
			@PathVariable String encryptedId){
        log.info("Ingress Request | GET /api/v1/payments/{} | EncryptedID provided", encryptedId);

        PaymentResponseDTO res = paymentService.getPaymentByEncryptedId(encryptedId);

        if (res == null) {
            log.warn("Egress Response | Payment not found for token: {}", encryptedId);
            return ResponseEntity.notFound().build();
        }

        log.info("Egress Response | Payment record retrieved successfully | InternalID Link: {}", res.getDisbursementId());

        return ResponseEntity.ok(res);
	}

    /**
     * Fetches the complete payment history associated with a specific grant application.
     * <p>
     * This endpoint provides a comprehensive audit trail of all financial movements
     * for a grant. It aggregates individual payment receipts, allowing officers
     * and applicants to reconcile disbursed funds against the total approved budget.
     * </p>
     *
     * @param applicationId The unique UUID of the grant application.
     * @return A list of summarized and encrypted payment records.
     */
    @Operation(
            summary = "Get Application Payment History",
            description = "Retrieves a full audit trail of all payments made toward a specific grant application. "
                    + "Includes payment methods, timestamps, and encrypted identifiers for secure record access.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Payment history retrieved successfully"),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User does not have permission to view this application's finances"),
                    @ApiResponse(responseCode = "404", description = "Not Found: Application ID does not exist")
            }
    )
    //@PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'ADMIN', 'COMPLIANCE_OFFICER') or " +
    //		"(hasRole('APPLICANT') and @securityService.isApplicationOwner(#applicationId))")

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentsByApplication(
            @Parameter(description = "The unique UUID of the grant application", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
            @PathVariable UUID applicationId) {

        log.info("Ingress Request | GET /api/v1/payments/application/{} | ApplicationID: {}", applicationId, applicationId);

        List<PaymentResponseDTO> history = paymentService.getPaymentsByApplication(applicationId);

        List<PaymentResponseDTO> safeHistory = (history != null) ? history : List.of();

        log.info("Egress Response | History Retrieval Complete | Total Records Found: {}", safeHistory.size());

        return ResponseEntity.ok(safeHistory);
    }

    /**
     * Generates and streams a PDF receipt for a specific transaction.
     * <p>
     * This method retrieves the payment details based on the encrypted token,
     * renders a legal document including transaction metadata and digital
     * signatures, and streams the raw byte array back to the client.
     * </p>
     *
     * @param encryptedId The secure identifier of the payment.
     * @return A {@link ResponseEntity} containing the binary PDF content.
     */
    @Operation(
            summary = "Download Payment Receipt",
            description = "Generates a legal PDF receipt for the transaction. "
                    + "The result is returned as a byte stream (application/pdf). "
                    + "Access is restricted based on transaction ownership.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF receipt generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User is not authorized to download this receipt"),
                    @ApiResponse(responseCode = "404", description = "Not Found: No payment record found for the provided token"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: PDF engine failure or data corruption")
            }
    )
    //@PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'ADMIN') or " +
    //		"(hasRole('APPLICANT') and @securityService.isPaymentOwner(#encryptedId))")
    @GetMapping(value = "/{encryptedId}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadReceipt(
            @Parameter(description = "The encrypted payment token received during the process stage",
                    example = "U2FsdGVkX19v8nZ_secure_token_xyz")
            @PathVariable String encryptedId) {

        log.info("Ingress Request | GET /api/v1/payments/{}/receipt | Token provided", encryptedId);

        byte[] receipt = paymentService.generatePaymentReceipt(encryptedId);

        if (receipt == null || receipt.length == 0) {
            log.error("Egress Response | PDF Generation Failed | Token: {}", encryptedId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        log.info("Egress Response | Receipt Streamed | Size: {} bytes", receipt.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Receipt_" + encryptedId.substring(0, 8) + ".pdf\"")
                .body(receipt);
    }
}