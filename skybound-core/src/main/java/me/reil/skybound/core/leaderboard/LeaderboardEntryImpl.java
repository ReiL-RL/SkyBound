package me.reil.skybound.core.leaderboard;

import me.reil.skybound.api.leaderboard.LeaderboardEntry;

import java.util.UUID;

public final class LeaderboardEntryImpl implements LeaderboardEntry {

    private final String islandId;
    private final String islandName;
    private final UUID owner;
    private final String ownerName;
    private final double value;
    private final int level;
    private int rank;
    private final int memberCount;

    public LeaderboardEntryImpl(String islandId, String islandName, UUID owner, String ownerName, double value, int level, int rank, int memberCount) {
        this.islandId = islandId;
        this.islandName = islandName;
        this.owner = owner;
        this.ownerName = ownerName;
        this.value = value;
        this.level = level;
        this.rank = rank;
        this.memberCount = memberCount;
    }

    @Override public String getIslandId() { return islandId; }
    @Override public String getIslandName() { return islandName; }
    @Override public UUID getOwner() { return owner; }
    @Override public String getOwnerName() { return ownerName; }
    @Override public double getValue() { return value; }
    @Override public int getLevel() { return level; }
    @Override public int getRank() { return rank; }
    @Override public int getMemberCount() { return memberCount; }

    public void setRank(int rank) { this.rank = rank; }
}
