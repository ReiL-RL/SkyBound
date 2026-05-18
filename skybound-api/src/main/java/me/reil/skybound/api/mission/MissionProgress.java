package me.reil.skybound.api.mission;

import java.util.Map;
import java.util.UUID;

/**
 * Tracks a player's progress on a specific mission.
 * Supports per-condition progress tracking.
 */
public interface MissionProgress {

    /** Player UUID. */
    UUID getPlayerId();

    /** Mission id. */
    String getMissionId();

    /** Progress per condition (conditionId -> current amount). */
    Map<String, Integer> getConditionProgress();

    /** Get progress for a specific condition. */
    int getConditionProgress(String conditionId);

    /** Whether the mission is completed. */
    boolean isCompleted();

    /** Whether the reward has been claimed. */
    boolean isClaimed();

    /** Overall completion percentage (0.0 - 1.0). */
    double getPercentage();

    /** Timestamp of completion (0 if not completed). */
    long getCompletedAt();

    /** Timestamp when mission was started/first progress (0 if never). */
    long getStartedAt();

    /** Timestamp of last completion (for cooldown tracking). */
    long getLastCompletedAt();
}
