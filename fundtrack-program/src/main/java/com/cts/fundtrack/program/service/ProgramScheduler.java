package com.cts.fundtrack.program.service; // Updated package

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// IMPORT FIXES: Point to Common and the new Program location
import com.cts.fundtrack.common.models.enums.ProgramStatus;
import com.cts.fundtrack.program.models.Program;
import com.cts.fundtrack.program.repository.ProgramRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled task runner responsible for automatically expiring grant funding programs
 * that have passed their end date.
 *
 * <p>This scheduler runs once per day at midnight (server time) and queries the database
 * for all programs that are currently in {@code ACTIVE} status but whose {@code endDate}
 * has already passed. Each such program is transitioned to {@code CLOSED} status in a
 * single batch save, ensuring applicants can no longer submit applications against them.</p>
 *
 * <p>Scheduling is enabled at the application level via {@code @EnableScheduling} on
 * {@link com.cts.fundtrack.program.FundtrackProgramApplication}.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProgramScheduler {

    private final ProgramRepository programRepository;

    /**
     * Automatically transitions all {@code ACTIVE} programs whose end date has passed
     * to {@code CLOSED} status.
     *
     * <p>This method is triggered by Spring's task scheduler according to the cron
     * expression {@code "0 0 0 * * *"}, which fires at exactly midnight every day.
     * The entire operation runs within a single database transaction: all qualifying
     * programs are fetched, their status updated in memory, then persisted together
     * via a batch {@code saveAll} call.</p>
     *
     * <p>If no programs require expiry on a given day, the method exits gracefully
     * without performing any writes.</p>
     */
    // Runs at midnight every day
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void autoExpirePrograms() {
        log.info("Starting scheduled task: Auto-expire programs.");

        LocalDate today = LocalDate.now();

        // Use the repository method we defined earlier
        List<Program> expiredPrograms = programRepository.findAllByStatusAndEndDateBefore(ProgramStatus.ACTIVE, today);

        if (!expiredPrograms.isEmpty()) {
            expiredPrograms.forEach(program -> {
                program.setStatus(ProgramStatus.CLOSED);
                log.info("Program ID {} has reached its end date and is now CLOSED.", program.getProgramId());
            });

            programRepository.saveAll(expiredPrograms);
            log.info("Successfully processed expiration of {} programs!", expiredPrograms.size());
        } else {
            log.info("There are no programs to expire today.");
        }
    }
}
