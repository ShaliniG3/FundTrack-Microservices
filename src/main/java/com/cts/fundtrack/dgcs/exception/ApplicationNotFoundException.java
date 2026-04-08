package com.cts.fundtrack.dgcs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested grant application cannot be located in the system.
 * <p>
 * This exception is typically triggered during retrieval operations by UUID or when
 * performing updates/actions on an application that does not exist in the database.
 * </p>
 * <p>
 * Associated with {@link HttpStatus#BAD_REQUEST} (400), though in some REST patterns,
 * it may also be mapped to {@link HttpStatus#NOT_FOUND} (404) depending on the
 * Global Exception Handler configuration.
 * </p>
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ApplicationNotFoundException extends RuntimeException {

        /**
         * Constructs a new ApplicationNotFoundException with a specific error message.
         *
         * @param message The detailed message explaining the context of the failure.
         */
        public ApplicationNotFoundException(String message) {
                super(message);
        }
}