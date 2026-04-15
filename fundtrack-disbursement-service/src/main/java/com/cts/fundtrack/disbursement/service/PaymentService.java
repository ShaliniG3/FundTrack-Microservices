package com.cts.fundtrack.disbursement.service;

import java.util.List;
import java.util.UUID;

import com.cts.fundtrack.common.dto.PaymentRequestDTO;
import com.cts.fundtrack.common.dto.PaymentResponseDTO;

/**
 * Service interface for processing and managing actual grant fund transactions.
 * <p>
 * While the Disbursement service manages the "plan," this service manages the 
 * "execution"—recording specific payment events, handling transaction security 
 * via encryption, and providing payment history.
 * </p>
 */
public interface PaymentService {

    PaymentResponseDTO processPayment(PaymentRequestDTO dto);


    PaymentResponseDTO getPaymentByEncryptedId(String encryptedPaymentId);

    List<PaymentResponseDTO> getPaymentsByApplication(UUID applicationId);


    byte[] generatePaymentReceipt(String encryptedPaymentId);
}
