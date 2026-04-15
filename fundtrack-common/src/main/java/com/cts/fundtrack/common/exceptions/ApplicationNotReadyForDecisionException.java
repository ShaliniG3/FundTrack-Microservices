package com.cts.fundtrack.common.exceptions;

import java.util.UUID;

// Specifically for when an Approver tries to process an application 
// that isn't in the correct initial state (e.g. still in DRAFT or already APPROVED)
public class ApplicationNotReadyForDecisionException extends RuntimeException {
    public ApplicationNotReadyForDecisionException(UUID id, String currentStatus) {
        super("Application " + id + " is in status '" + currentStatus + 
            "'. Only applications UNDER_REVIEW can be finalized.");
    }
}