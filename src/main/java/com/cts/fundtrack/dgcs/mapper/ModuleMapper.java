package com.cts.fundtrack.dgcs.mapper;

import com.cts.fundtrack.dgcs.config.EncryptionUtil;
import com.cts.fundtrack.dgcs.dto.disbursementdto.DisbursementResponseDTO;
import com.cts.fundtrack.dgcs.dto.paymentdto.PaymentResponseDTO;
import com.cts.fundtrack.dgcs.model.Disbursement;
import com.cts.fundtrack.dgcs.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ModuleMapper {

    @Autowired
    protected EncryptionUtil encryptionUtil;

    // FIX 1: Map directly from disbursementId and applicationId fields
    @Mapping(target = "id", source = "disbursementId")
    @Mapping(target = "applicationId", source = "applicationId")
    public abstract DisbursementResponseDTO toDisbursementResponseDTO(Disbursement entity);

    // FIX 2: If 'payment' contains 'disbursementId' directly, use that.
    // If it is nested, use 'payment.disbursementId' to be explicit.
    @Mapping(target = "encryptedPaymentId", expression = "java(encryptionUtil.encrypt(payment.getPaymentId()))")
    @Mapping(target = "disbursementId", source = "payment.disbursementId")
    public abstract PaymentResponseDTO toPaymentResponseDTO(Payment payment);

}