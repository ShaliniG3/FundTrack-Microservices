package com.cts.fundtrack.dgcs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user with the 'APPROVER' role cannot be found or identified.
 * <p>
 * This is typically used in the approval workflow when assigning a review to an
 * authorized approver, or when validating the existence of an approver tied
 * to a specific program or application.
 * </p>
 * * <p>Returning a {@link HttpStatus#BAD_REQUEST} ensures the client is notified
 * that the request cannot proceed due to the missing required authority.</p>
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ApproverNotFoundException extends RuntimeException {

        /**
         * Constructs a new ApproverNotFoundException with a detailed error message.
         *
         * @param message the detail message describing which approver was missing
         * or the context of the failure.
         */
        public ApproverNotFoundException(String message) {
                super(message);
        }
}