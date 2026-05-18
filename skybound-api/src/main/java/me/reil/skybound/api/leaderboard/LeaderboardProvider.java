package me.reil.skybound.api.leaderboard;

import java.util.List;

/**
 * Leaderboard/Top Islands provider.
 * Supports sorting by value, level, or custom criteria.
 */
public interface LeaderboardProvider {

    /**
     * Get top islands by value.
     * @param limit max entries
     * @return ordered list of entries (highest first)
     */
    List<LeaderboardEntry> getTopByValue(int limit);

    /**
     * Get top islands by level.
     * @param limit max entries
     * @return ordered list of entries (highest first)
     */
    List<LeaderboardEntry> getTopByLevel(int limit);

    /**
     * Get an island's rank by value.
     * @return 1-based rank, or -1 if not found
     */
    int getRankByValue(String islandId);

    /**
     * Get an island's rank by level.
     * @return 1-based rank, or -1 if not found
     */
    int getRankByLevel(String islandId);

    /**
     * Force recalculation of leaderboard.
     */
    void recalculate();
}
