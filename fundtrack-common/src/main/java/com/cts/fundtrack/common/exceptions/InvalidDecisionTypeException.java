package com.cts.fundtrack.common.exceptions;

public class InvalidDecisionTypeException extends RuntimeException {
    public InvalidDecisionTypeException(String decision) {
        super("'" + decision + "' is not a valid final decision. Use 'APPROVED' or 'REJECTED'.");
    }
}