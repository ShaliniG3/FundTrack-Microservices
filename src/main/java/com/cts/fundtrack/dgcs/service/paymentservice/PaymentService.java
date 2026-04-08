package com.cts.fundtrack.dgcs.service.paymentservice;

import com.cts.fundtrack.dgcs.dto.paymentdto.PaymentRequestDTO;
import com.cts.fundtrack.dgcs.dto.paymentdto.PaymentResponseDTO;

import java.util.List;
import java.util.UUID;

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


