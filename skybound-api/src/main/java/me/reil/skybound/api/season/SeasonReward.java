package me.reil.skybound.api.season;

import java.util.List;

/**
 * Reward for a specific rank at end of season.
 */
public interface SeasonReward {

    /** Rank this reward is for (1 = first place). */
    int getRank();

    /** Commands to execute ({player} placeholder). */
    List<String> getCommands();
}
