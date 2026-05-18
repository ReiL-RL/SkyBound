package me.reil.skybound.api.season;

/**
 * Represents a season time period.
 */
public interface Season {

    /** Season number/id. */
    int getNumber();

    /** Season start timestamp. */
    long getStartTime();

    /** Season end timestamp (calculated). */
    long getEndTime();

    /** Duration in days. */
    int getDurationDays();

    /** Whether this season is currently active. */
    boolean isActive();
}
