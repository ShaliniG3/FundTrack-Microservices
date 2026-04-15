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

@Service
@Slf4j
@RequiredArgsConstructor
public class ProgramScheduler {
    
    private final ProgramRepository programRepository;
    
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