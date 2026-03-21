package com.fundtrack.audit_service.exceptions;


public class UserIdNotFoundException extends RuntimeException {
    public UserIdNotFoundException(String message ) {
        super(message);
    }
}