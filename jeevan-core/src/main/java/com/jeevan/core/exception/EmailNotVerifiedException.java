package com.jeevan.core.exception;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("Please verify your email before booking an appointment.");
    }
}
