package com.cts.fundtrack.common.exceptions;

public class UnsupportedDocumentTypeException extends RuntimeException {
    public UnsupportedDocumentTypeException(String message) {
        super(message);
    }
}