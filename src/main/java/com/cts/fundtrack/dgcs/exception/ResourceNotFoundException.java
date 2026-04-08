package com.cts.fundtrack.dgcs.exception;
/**
 * Custom exception thrown when a requested resource (e.g., Program, User, or Eligibility Rule) 
 * cannot be located in the persistence layer.
 * <p>
 * Extending {@link RuntimeException} allows this exception to be unchecked, ensuring 
 * cleaner method signatures while still triggering a transaction rollback in 
 * Spring's {@code @Transactional} context.
 * </p>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new ResourceNotFoundException with a detailed message.
     * This message is typically mapped to the 'message' field in the 
     * {@link ErrorResponse} and returned as an {@code HTTP 404 Not Found}.
     *
     * @param message Descriptive explanation including the unique identifier 
     * (UUID) of the missing resource.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
    
