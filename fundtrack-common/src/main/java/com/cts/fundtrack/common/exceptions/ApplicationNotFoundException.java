package com.cts.fundtrack.common.exceptions;

import java.util.UUID;

// 1. When a UUID doesn't exist in the database
public class ApplicationNotFoundException extends RuntimeException {
    public ApplicationNotFoundException(UUID id) {
        super("Application with ID " + id + " was not found.");
    }
}