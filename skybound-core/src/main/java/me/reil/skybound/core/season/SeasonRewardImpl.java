package me.reil.skybound.core.season;

import me.reil.skybound.api.season.SeasonReward;

import java.util.Collections;
import java.util.List;

public final class SeasonRewardImpl implements SeasonReward {

    private final int rank;
    private final List<String> commands;

    public SeasonRewardImpl(int rank, List<String> commands) {
        this.rank = rank;
        this.commands = commands != null ? commands : Collections.<String>emptyList();
    }

    @Override
    public int getRank() { return rank; }

    @Override
    public List<String> getCommands() { return Collections.unmodifiableList(commands); }
}
