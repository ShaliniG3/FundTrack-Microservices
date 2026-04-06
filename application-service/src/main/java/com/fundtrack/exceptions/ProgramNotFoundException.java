package com.fundtrack.exceptions;

import java.util.UUID;

public class ProgramNotFoundException extends RuntimeException {
    public ProgramNotFoundException(UUID programId) {
        super("Grant Program with ID " + programId + " does not exist or is inactive.");
    }
}
