package me.reil.skybound.api.season;

import java.util.List;

/**
 * Season system provider.
 * Seasons are time periods with leaderboard rewards.
 */
public interface SeasonProvider {

    /** Get the current active season. */
    Season getCurrentSeason();

    /** Get remaining time in milliseconds. */
    long getRemainingTime();

    /** Get configured rewards for season end. */
    List<SeasonReward> getRewards();

    /** Manually end the current season (admin). */
    void endSeason();

    /** Whether the season system is enabled. */
    boolean isEnabled();
}
