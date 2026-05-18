package me.reil.skybound.core.island;

import java.util.UUID;

/**
 * A single log entry for island activity tracking.
 */
public final class IslandLogEntry {

    private final long timestamp;
    private final UUID playerId;
    private final String playerName;
    private final LogAction action;
    private final String details;

    public IslandLogEntry(long timestamp, UUID playerId, String playerName, LogAction action, String details) {
        this.timestamp = timestamp;
        this.playerId = playerId;
        this.playerName = playerName;
        this.action = action;
        this.details = details;
    }

    public long getTimestamp() { return timestamp; }
    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public LogAction getAction() { return action; }
    public String getDetails() { return details; }

    public enum LogAction {
        MEMBER_JOIN,
        MEMBER_LEAVE,
        MEMBER_KICK,
        UPGRADE_PURCHASE,
        BOOSTER_PURCHASE,
        BANK_DEPOSIT,
        BANK_WITHDRAW,
        WARP_SET,
        WARP_REMOVE,
        SETTINGS_CHANGE,
        ISLAND_LOCK,
        ISLAND_UNLOCK,
        ISLAND_RENAME
    }
}
