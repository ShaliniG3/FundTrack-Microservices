package com.cts.fundtrack.disbursement.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.cts.fundtrack.common.dto.DisbursementResponseDTO;
import com.cts.fundtrack.common.dto.PaymentResponseDTO;
import com.cts.fundtrack.disbursement.models.Disbursement;
import com.cts.fundtrack.disbursement.models.Payment;
import com.cts.fundtrack.disbursement.util.EncryptionUtil;

/**
 * MapStruct mapper for converting between disbursement-domain JPA entities and their
 * corresponding response DTOs.
 * <p>
 * This abstract class is used as a Spring bean (via {@code componentModel = "spring"})
 * and leverages MapStruct's compile-time code generation. It injects
 * {@link EncryptionUtil} to AES-encrypt payment UUIDs before they are included in
 * outbound response DTOs, ensuring raw database identifiers are never exposed to
 * API consumers.
 * </p>
 */
@Mapper(componentModel = "spring")
public abstract class ModuleMapper {

    /**
     * AES encryption utility injected by Spring to encrypt payment UUIDs in outbound DTOs.
     */
    @Autowired
    protected EncryptionUtil encryptionUtil;

    /**
     * Maps a {@link Disbursement} entity to a {@link DisbursementResponseDTO}.
     * <p>
     * The entity's {@code disbursementId} field is mapped to the DTO's {@code id} field
     * to match the established API contract for disbursement responses.
     * </p>
     *
     * @param entity the source {@link Disbursement} JPA entity
     * @return a {@link DisbursementResponseDTO} populated with the entity's fields
     */
    @Mapping(target = "id", source = "entity.disbursementId")
    public abstract DisbursementResponseDTO toDisbursementResponseDTO(Disbursement entity);

    /**
     * Maps a {@link Payment} entity to a {@link PaymentResponseDTO} with AES-encrypted ID.
     * <p>
     * The entity's raw {@code paymentId} UUID is encrypted via {@link EncryptionUtil#encrypt(java.util.UUID)}
     * and stored in the DTO's {@code encryptedPaymentId} field, so clients receive only
     * the obfuscated reference. The {@code disbursementId} is mapped directly.
     * </p>
     *
     * @param payment the source {@link Payment} JPA entity
     * @return a {@link PaymentResponseDTO} with an encrypted payment identifier
     */
    @Mapping(target = "encryptedPaymentId", expression = "java(encryptionUtil.encrypt(payment.getPaymentId()))")
    @Mapping(target = "disbursementId", source = "payment.disbursementId")
    public abstract PaymentResponseDTO toPaymentResponseDTO(Payment payment);
}