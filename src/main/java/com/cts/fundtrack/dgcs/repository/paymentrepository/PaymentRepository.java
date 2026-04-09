package com.cts.fundtrack.dgcs.repository.paymentrepository;
import com.cts.fundtrack.dgcs.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing actual financial transaction records.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Finds all payment attempts linked to a specific scheduled installment.
     */
 //   List<Payment> findByDisbursement_DisbursementId(UUID disbursementId);
//    @Query("SELECT COUNT(p) > 0 FROM Payment p " +
//            "WHERE p.paymentId = :paymentId " +
//            "AND p.disbursement.application.applicant.email = :email")
//    boolean existsByPaymentIdAndUserEmail(@Param("paymentId") UUID paymentId, @Param("email") String email);
    /**
     * Finds all payments for a specific application.
     * Used for the application history view.
     */
  //  List<Payment> findByDisbursement_Application_ApplicationId(UUID applicationId);
    List<Payment> findAllByDisbursementIdIn(List<UUID> disbursementIds);

}
