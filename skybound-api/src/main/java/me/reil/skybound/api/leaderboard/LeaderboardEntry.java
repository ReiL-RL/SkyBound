package me.reil.skybound.api.leaderboard;

import java.util.UUID;

/**
 * A single entry in the leaderboard.
 */
public interface LeaderboardEntry {

    /** Island id. */
    String getIslandId();

    /** Island name. */
    String getIslandName();

    /** Owner UUID. */
    UUID getOwner();

    /** Owner name. */
    String getOwnerName();

    /** Island value. */
    double getValue();

    /** Island level. */
    int getLevel();

    /** Rank position (1-based). */
    int getRank();

    /** Member count. */
    int getMemberCount();
}
