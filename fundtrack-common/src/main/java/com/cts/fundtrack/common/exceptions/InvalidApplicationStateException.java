package com.cts.fundtrack.common.exceptions;

public class InvalidApplicationStateException extends RuntimeException {
    public InvalidApplicationStateException(String status) {
        super("Cannot modify application. Current status '" + status + "' is final.");
    }
}