package com.cts.fundtrack.disbursement.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.cts.fundtrack.common.dto.DisbursementResponseDTO;
import com.cts.fundtrack.common.dto.PaymentResponseDTO;
import com.cts.fundtrack.disbursement.models.Disbursement;
import com.cts.fundtrack.disbursement.models.Payment;
import com.cts.fundtrack.disbursement.util.EncryptionUtil;

@Mapper(componentModel = "spring")
public abstract class ModuleMapper {

    @Autowired
    protected EncryptionUtil encryptionUtil;

    /**
     * Maps Disbursement Entity to Response DTO.
     * Target is changed to "id" to match the existing DTO contract.
     */
    @Mapping(target = "id", source = "entity.disbursementId")
    public abstract DisbursementResponseDTO toDisbursementResponseDTO(Disbursement entity);

    /**
     * Maps Payment Entity to Response DTO with Encryption.
     * Note: Ensure PaymentResponseDTO also uses "id" or "paymentId" correctly.
     */
    @Mapping(target = "encryptedPaymentId", expression = "java(encryptionUtil.encrypt(payment.getPaymentId()))")
    @Mapping(target = "disbursementId", source = "payment.disbursementId")
    public abstract PaymentResponseDTO toPaymentResponseDTO(Payment payment);
}