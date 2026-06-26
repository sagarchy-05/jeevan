package com.jeevan.core.exception;

public class SlotAlreadyBookedException extends RuntimeException {

    public SlotAlreadyBookedException() {
        super("That slot was just taken.");
    }
}
