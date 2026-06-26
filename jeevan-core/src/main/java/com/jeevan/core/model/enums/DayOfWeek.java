package com.jeevan.core.model.enums;

/**
 * Weekday for recurring availability. Declared MONDAY..SUNDAY so the ordinal is
 * the natural weekday order, and the names match {@link java.time.DayOfWeek}
 * (and the seed values), allowing {@code DayOfWeek.valueOf(localDate.getDayOfWeek().name())}.
 */
public enum DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}
