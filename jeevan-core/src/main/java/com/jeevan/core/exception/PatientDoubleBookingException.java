package com.jeevan.core.exception;

public class PatientDoubleBookingException extends RuntimeException {

    public PatientDoubleBookingException() {
        super("You already have an appointment at that time.");
    }
}
