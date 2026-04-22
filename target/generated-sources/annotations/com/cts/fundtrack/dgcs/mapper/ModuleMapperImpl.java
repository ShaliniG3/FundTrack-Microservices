package com.cts.fundtrack.dgcs.mapper;

import com.cts.fundtrack.dgcs.dto.disbursementdto.DisbursementResponseDTO;
import com.cts.fundtrack.dgcs.dto.paymentdto.PaymentResponseDTO;
import com.cts.fundtrack.dgcs.model.Disbursement;
import com.cts.fundtrack.dgcs.model.Payment;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-09T14:44:00+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21 (Oracle Corporation)"
)
@Component
public class ModuleMapperImpl extends ModuleMapper {

    @Override
    public DisbursementResponseDTO toDisbursementResponseDTO(Disbursement entity) {
        if ( entity == null ) {
            return null;
        }

        DisbursementResponseDTO.DisbursementResponseDTOBuilder disbursementResponseDTO = DisbursementResponseDTO.builder();

        disbursementResponseDTO.id( entity.getDisbursementId() );
        disbursementResponseDTO.applicationId( entity.getApplicationId() );
        disbursementResponseDTO.amount( entity.getAmount() );
        disbursementResponseDTO.scheduledDate( entity.getScheduledDate() );
        disbursementResponseDTO.status( entity.getStatus() );

        return disbursementResponseDTO.build();
    }

    @Override
    public PaymentResponseDTO toPaymentResponseDTO(Payment payment) {
        if ( payment == null ) {
            return null;
        }

        PaymentResponseDTO.PaymentResponseDTOBuilder paymentResponseDTO = PaymentResponseDTO.builder();

        paymentResponseDTO.disbursementId( payment.getDisbursementId() );
        paymentResponseDTO.amount( payment.getAmount() );
        paymentResponseDTO.date( payment.getDate() );
        paymentResponseDTO.method( payment.getMethod() );
        paymentResponseDTO.status( payment.getStatus() );

        paymentResponseDTO.encryptedPaymentId( encryptionUtil.encrypt(payment.getPaymentId()) );

        return paymentResponseDTO.build();
    }
}
