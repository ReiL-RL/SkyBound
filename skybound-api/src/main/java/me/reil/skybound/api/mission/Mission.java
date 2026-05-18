package me.reil.skybound.api.mission;

import org.bukkit.Material;

import java.util.List;

/**
 * Represents a mission definition.
 * Missions support multiple conditions with AND/OR logic,
 * prerequisites, cooldowns, time limits, and tiered rewards.
 */
public interface Mission {

    /** Unique mission id. */
    String getId();

    /** Display name. */
    String getDisplayName();

    /** Description/lore lines. */
    List<String> getDescription();

    /** Mission conditions. */
    List<MissionCondition> getConditions();

    /** How conditions are evaluated (AND = all, OR = any one). */
    ConditionMode getConditionMode();

    /** Money reward. */
    double getMoneyReward();

    /** Island XP reward. */
    long getXpReward();

    /** Command rewards (executed on completion). */
    List<String> getCommandRewards();

    /** Item rewards (material:amount format). */
    List<String> getItemRewards();

    /** Minimum island level required to unlock this mission. */
    int getRequiredLevel();

    /** Whether this mission can be repeated. */
    boolean isRepeatable();

    /** Cooldown in seconds between repeats (0 = no cooldown). */
    int getCooldownSeconds();

    /** Time limit in seconds to complete (0 = no limit). */
    int getTimeLimitSeconds();

    /** Prerequisite mission ids that must be completed first. */
    List<String> getPrerequisites();

    /** Icon material for GUI. */
    Material getIcon();

    /** Mission category/tier for GUI grouping. */
    String getCategory();

    /**
     * Reset type for daily/weekly missions.
     * @return null for permanent, "daily" for daily reset, "weekly" for weekly reset
     */
    String getResetType();
}
