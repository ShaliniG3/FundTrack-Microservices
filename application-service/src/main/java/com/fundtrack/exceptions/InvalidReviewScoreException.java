package com.fundtrack.exceptions;

public class InvalidReviewScoreException extends RuntimeException {
    public InvalidReviewScoreException(Integer score) {
        super("The provided score '" + score + "' is invalid. Score must be between 0 and 100.");
    }
}
