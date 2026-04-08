package com.cts.fundtrack.dgcs.repository.grantreportrepository;


import com.cts.fundtrack.dgcs.model.GrantReport;
import com.cts.fundtrack.dgcs.model.enums.GrantReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface GrantReportRepository extends JpaRepository<GrantReport, UUID> {

    /**
     * Retrieves reports linked to a specific Application ID.
     * Refactored for Microservices: No longer joins with the Application table.
     */
    List<GrantReport> findByApplicationId(UUID applicationId);

    /**
     * Counts reports for a designated Application ID.
     */
    long countByApplicationId(UUID applicationId);

    /**
     * Finds reports by Application ID ordered by most recent first.
     */
    List<GrantReport> findByApplicationIdOrderBySubmittedDateDesc(UUID applicationId);

    /**
     * Counts reports by Application ID and a specific status.
     */
    long countByApplicationIdAndStatus(UUID applicationId, GrantReportStatus status);


    }