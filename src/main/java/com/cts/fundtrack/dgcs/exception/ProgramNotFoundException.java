package com.cts.fundtrack.dgcs.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * Exception thrown when a requested program (such as a funding program,
 * course, or domain entity) cannot be found in the system.
 * <p>
 * This is typically thrown by service or repository layers when a lookup
 * based on an ID or other key returns no matching program.
 * <p>
 * A WARN‑level log entry is written to aid debugging and traceability.
 */
@Slf4j
public class ProgramNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code ProgramNotFoundException} with the specified detail message.
     *
     * @param message a descriptive message indicating which program was not found
     */
    public ProgramNotFoundException(String message) {
        super(message);
        log.warn("ProgramNotFoundException created: {}", message);
    }
}


