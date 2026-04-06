package com.fundtrack.exceptions;

import java.util.UUID;

public class DuplicateApplicationException extends RuntimeException {
    public DuplicateApplicationException(UUID applicantId, UUID programId) {
        super("Applicant " + applicantId + " has already submitted an application for program " + programId);
    }
}
