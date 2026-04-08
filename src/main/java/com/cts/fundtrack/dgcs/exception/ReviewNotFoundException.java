package com.cts.fundtrack.dgcs.exception;

public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(String message){
        super(message);
    }
}
