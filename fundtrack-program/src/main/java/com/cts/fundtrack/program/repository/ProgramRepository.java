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

/**
 * Spring Data JPA repository for {@link Program} entities.
 *
 * <p>Extends {@link JpaRepository} to inherit standard CRUD operations and pagination
 * support. This interface also declares custom derived queries and a JPQL query to
 * support the Program Microservice's filtering, search, and scheduling requirements.</p>
 *
 * <p>Key query responsibilities:</p>
 * <ul>
 *   <li>Role-based filtering — restricting applicants to visible program statuses.</li>
 *   <li>Keyword search — case-insensitive matching across program name and description,
 *       with optional status filtering for applicant-facing searches.</li>
 *   <li>Scheduled expiry — identifying active programs whose end date has passed,
 *       used by {@link com.cts.fundtrack.program.service.ProgramScheduler}.</li>
 * </ul>
 */
@Repository
public interface ProgramRepository extends JpaRepository<Program, UUID> {

    /**
     * Retrieves all programs that have the specified lifecycle status.
     *
     * @param status the {@link ProgramStatus} to filter by.
     * @return a list of {@link Program} entities matching the given status;
     *         empty list if none found.
     */
    // Simple status filter
    List<Program> findByStatus(ProgramStatus status);

    /**
     * Retrieves all programs whose lifecycle status is contained in the provided collection.
     *
     * <p>Used to present applicants with only the subset of programs they are permitted
     * to view (typically {@code ACTIVE} and {@code CLOSED}).</p>
     *
     * @param statuses a collection of {@link ProgramStatus} values to include.
     * @return a list of {@link Program} entities whose status is one of the given values;
     *         empty list if none found.
     */
    // Filter by multiple statuses (e.g., ACTIVE and CLOSED)
    List<Program> findByStatusIn(Collection<ProgramStatus> statuses);

    /**
     * Retrieves all programs whose end date is strictly after the given date.
     *
     * <p>Useful for finding programs that are still within their active funding window.</p>
     *
     * @param now the reference date; programs with an {@code endDate} after this date
     *            are returned.
     * @return a list of {@link Program} entities that have not yet reached their end date;
     *         empty list if none found.
     */
    // Find programs that haven't expired yet
    List<Program> findByEndDateAfter(LocalDate now);

    /**
     * Retrieves all programs with the given status whose end date is strictly before
     * the given date.
     *
     * <p>Used by {@link com.cts.fundtrack.program.service.ProgramScheduler} to identify
     * {@code ACTIVE} programs that have exceeded their end date and should be automatically
     * transitioned to {@code CLOSED} status.</p>
     *
     * @param status the {@link ProgramStatus} that the program must currently have
     *               (typically {@code ACTIVE}).
     * @param date   the reference date; programs with an {@code endDate} strictly before
     *               this date are returned.
     * @return a list of {@link Program} entities that are past their end date;
     *         empty list if none found.
     */
    // Find programs past their deadline for automated cleanup
    List<Program> findAllByStatusAndEndDateBefore(ProgramStatus status, LocalDate date);

    /**
     * Searches for programs by a keyword within a restricted set of statuses.
     *
     * <p>Performs a case-insensitive LIKE match against both the program's {@code name}
     * and {@code description} fields, and only returns results whose status is in the
     * provided collection. Used for applicant-facing keyword searches, where only
     * {@code ACTIVE} and {@code CLOSED} programs should be visible.</p>
     *
     * @param keyword  the search term to match (case-insensitive, partial match supported).
     * @param statuses a collection of {@link ProgramStatus} values to restrict results to.
     * @return a list of matching {@link Program} entities; empty list if none found.
     */
    // Keyword search across Name and Description (Case Insensitive)
    @Query("SELECT p FROM Program p WHERE p.status IN :statuses AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Program> searchByKeywordAndStatusIn(@Param("keyword") String keyword,
                                            @Param("statuses") Collection<ProgramStatus> statuses);

    /**
     * Searches for programs by a keyword across name and description fields, with no
     * status restriction.
     *
     * <p>Used for admin and staff keyword searches, where all programs regardless of
     * status should be searchable. Both the {@code name} and {@code description} parameters
     * should be passed the same keyword value to perform an OR match across both columns.</p>
     *
     * @param name        the keyword to match against the program's {@code name} field
     *                    (case-insensitive, partial match).
     * @param description the keyword to match against the program's {@code description}
     *                    field (case-insensitive, partial match).
     * @return a list of matching {@link Program} entities; empty list if none found.
     */
    // Staff search helper
    List<Program> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);
}
