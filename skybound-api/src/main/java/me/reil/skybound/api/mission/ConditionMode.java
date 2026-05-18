package me.reil.skybound.api.mission;

/**
 * How multiple conditions are evaluated.
 */
public enum ConditionMode {

    /** All conditions must be met. */
    AND,

    /** At least one condition must be met. */
    OR
}
