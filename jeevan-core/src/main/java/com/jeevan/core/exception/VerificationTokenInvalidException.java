package com.jeevan.core.exception;

public class VerificationTokenInvalidException extends RuntimeException {

    public VerificationTokenInvalidException() {
        super("This verification link is invalid or has expired.");
    }
}
