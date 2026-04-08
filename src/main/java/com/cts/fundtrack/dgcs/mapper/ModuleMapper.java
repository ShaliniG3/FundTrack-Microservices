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

    @Mapping(target = "id", source = "disbursementId")
    @Mapping(target = "applicationId", source = "application.applicationId")
    public abstract DisbursementResponseDTO toDisbursementResponseDTO(Disbursement entity);

    @Mapping(target = "encryptedPaymentId", expression = "java(encryptionUtil.encrypt(payment.getPaymentId()))")
    @Mapping(target = "disbursementId", source = "disbursement.disbursementId")
    public abstract PaymentResponseDTO toPaymentResponseDTO(Payment payment);

}