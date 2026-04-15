package com.cts.fundtrack.common.exceptions;

import java.util.UUID;

// Specifically for when a reviewer tries to access an application 
// that hasn't passed the initial validation phase.
public class ApplicationNotSubmittedException extends RuntimeException {
    public ApplicationNotSubmittedException(UUID id, String currentStatus) {
        super("Application " + id + " is in status '" + currentStatus + 
            "'. Only applications in 'SUBMITTED' status can be reviewed.");
    }
}