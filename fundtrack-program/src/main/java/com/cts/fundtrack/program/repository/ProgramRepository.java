package com.cts.fundtrack.program.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cts.fundtrack.common.models.enums.ProgramStatus;
import com.cts.fundtrack.program.models.Program;

@Repository
public interface ProgramRepository extends JpaRepository<Program, UUID> {

    // Simple status filter
    List<Program> findByStatus(ProgramStatus status);

    // Filter by multiple statuses (e.g., ACTIVE and CLOSED)
    List<Program> findByStatusIn(Collection<ProgramStatus> statuses);

    // Find programs that haven't expired yet
    List<Program> findByEndDateAfter(LocalDate now);

    // Find programs past their deadline for automated cleanup
    List<Program> findAllByStatusAndEndDateBefore(ProgramStatus status, LocalDate date);

    // Keyword search across Name and Description (Case Insensitive)
    @Query("SELECT p FROM Program p WHERE p.status IN :statuses AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Program> searchByKeywordAndStatusIn(@Param("keyword") String keyword, 
                                            @Param("statuses") Collection<ProgramStatus> statuses);

    // Staff search helper
    List<Program> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);
}