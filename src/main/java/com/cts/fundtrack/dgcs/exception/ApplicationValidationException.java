package com.cts.fundtrack.dgcs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when business rules fail, such as incorrect document counts.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class ApplicationValidationException extends RuntimeException {
    public ApplicationValidationException(String message) {
        super(message);
    }
}


