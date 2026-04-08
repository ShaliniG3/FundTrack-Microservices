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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
	//@PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'ADMIN')")
	@PostMapping("/process")
	@Operation(
			summary = "Process Disbursement Payment",
			description = "Initiates the transfer of funds for a specific disbursement ID and records the transaction metadata.",
			responses = {
					@ApiResponse(responseCode = "201", description = "Payment processed successfully"),
					@ApiResponse(responseCode = "400", description = "Invalid payment data or disbursement already paid"),
					@ApiResponse(responseCode = "422", description = "Unprocessable entity - Insufficient program budget")
			}
	)
	public ResponseEntity<PaymentResponseDTO> processPayment(@Valid @RequestBody PaymentRequestDTO dto){
		log.info("Processing the payment for Disbursement: {}", dto.getDisbursementId());
		PaymentResponseDTO res = paymentService.processPayment(dto);
		return new ResponseEntity<>(res, HttpStatus.CREATED);
	}

	/**
	 * Retrieves detailed payment information using a secure encrypted identifier.
	 *
	 * @param encryptedId The AES-encrypted string representing the Payment ID.
	 * @return A {@link ResponseEntity} containing the decrypted payment record.
	 */
	//@PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'ADMIN', 'COMPLIANCE_OFFICER') or " +
		//	"(hasRole('APPLICANT') and @securityService.isPaymentOwner(#encryptedId))")
	@GetMapping("/{encryptedId}")
	@Operation(
			summary = "Get Payment by Secure ID",
			description = "Retrieves a specific payment record. Accepts an encrypted ID to prevent ID enumeration attacks.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Payment found and decrypted successfully"),
					@ApiResponse(responseCode = "404", description = "Payment record not found")
			}
	)
	public ResponseEntity<PaymentResponseDTO> getPaymentByEncryptedId(
			@Parameter(description = "Encrypted Payment Identifier", example = "U2FsdGVkX1+...")
			@PathVariable String encryptedId){
		log.debug("REST request to get payment by encrypted ID: {}", encryptedId);
		PaymentResponseDTO res = paymentService.getPaymentByEncryptedId(encryptedId);
		return ResponseEntity.ok(res);
	}

	/**
	 * Fetches the complete payment history associated with a specific grant application.
	 *
	 * @param applicationId The unique UUID of the grant application.
	 * @return A list of summarized payment records.
	 */
	//@PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'ADMIN', 'COMPLIANCE_OFFICER') or " +
	//		"(hasRole('APPLICANT') and @securityService.isApplicationOwner(#applicationId))")
	@GetMapping("/application/{applicationId}")
	@Operation(
			summary = "Get Application Payment History",
			description = "Retrieves a full audit trail of all payments made toward a specific grant application."
	)
	public ResponseEntity<List<PaymentResponseDTO>> getPaymentsByApplication(
			@Parameter(description = "UUID of the application", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
			@PathVariable UUID applicationId){
		log.info("REST request to fetch payment history for application: {}", applicationId);
		List<PaymentResponseDTO> res = paymentService.getPaymentsByApplication(applicationId);
		return ResponseEntity.ok(res);
	}

	/**
	 * Generates and streams a PDF receipt for a specific transaction.
	 *
	 * @param encryptedId The secure identifier of the payment.
	 * @return A {@link ResponseEntity} containing the binary PDF content.
	 */
	//@PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'ADMIN') or " +
	//		"(hasRole('APPLICANT') and @securityService.isPaymentOwner(#encryptedId))")
	@GetMapping(value = "/{encryptedId}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
	@Operation(
			summary = "Download Payment Receipt",
			description = "Generates a legal PDF receipt for the transaction. The result is returned as a byte stream (application/pdf).",
			responses = {
					@ApiResponse(responseCode = "200", description = "PDF receipt generated successfully",
							content = @Content(mediaType = "application/pdf")),
					@ApiResponse(responseCode = "500", description = "Error during PDF generation logic")
			}
	)
	public ResponseEntity<byte[]> downloadReceipt(
			@Parameter(description = "Encrypted Payment Identifier") @PathVariable String encryptedId){
		log.info("REST request to generate receipt for payment ID: {}", encryptedId);
		byte[] receipt = paymentService.generatePaymentReceipt(encryptedId);
		return ResponseEntity.ok(receipt);
	}
}