package me.reil.skybound.api.mission;

/**
 * A single condition within a mission.
 * Missions can have multiple conditions combined with AND/OR logic.
 */
public interface MissionCondition {

    /** Unique condition id within the mission. */
    String getId();

    /** Condition type (BREAK_BLOCK, KILL_MOB, etc). */
    MissionType getType();

    /**
     * Target filter (material name, entity type, or empty for "any").
     * Examples: "DIAMOND_ORE", "ZOMBIE", "" (any)
     */
    String getTarget();

    /** Required amount to satisfy this condition. */
    int getAmount();
}
