package com.cts.fundtrack.common.exceptions;

public class InvalidProgramStateException extends RuntimeException{
	
	public InvalidProgramStateException(String message) {
		super(message);
	}
}