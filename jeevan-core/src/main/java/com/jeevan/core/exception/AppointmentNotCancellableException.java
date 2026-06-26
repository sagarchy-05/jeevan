package com.jeevan.core.exception;

public class AppointmentNotCancellableException extends RuntimeException {

    public AppointmentNotCancellableException(String message) {
        super(message);
    }
}
