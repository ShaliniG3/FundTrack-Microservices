package com.cts.fundtrack.disbursement.repository;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cts.fundtrack.disbursement.models.Payment;

/**
 * Spring Data JPA repository for {@link Payment} entities.
 * <p>
 * Provides data access operations for actual financial transaction records.
 * Each {@link Payment} represents a realized fund transfer that settles one
 * disbursement installment. This repository supports payment history retrieval
 * and is used by {@link com.cts.fundtrack.disbursement.service.PaymentServiceImpl}
 * to load payment records for display and receipt generation.
 * </p>
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Retrieves all payment records whose disbursement ID matches any entry in the
     * provided list. Used to bulk-load all payments associated with a grant application's
     * full set of disbursement installments for the payment history view.
     *
     * @param disbursementIds the list of disbursement UUIDs to match against
     * @return a list of {@link Payment} entities linked to any of the given disbursement IDs;
     *         an empty list if none are found
     */
    List<Payment> findAllByDisbursementIdIn(List<UUID> disbursementIds);

}