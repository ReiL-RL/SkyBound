package me.reil.skybound.api.event;

import me.reil.skybound.api.island.Island;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired when a player leaves an island team.
 */
public class IslandMemberLeaveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Island island;
    private final UUID playerId;
    private final LeaveReason reason;

    public IslandMemberLeaveEvent(Island island, UUID playerId, LeaveReason reason) {
        this.island = island;
        this.playerId = playerId;
        this.reason = reason;
    }

    public Island getIsland() { return island; }
    public UUID getPlayerId() { return playerId; }
    public LeaveReason getReason() { return reason; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }

    public enum LeaveReason {
        LEAVE,
        KICKED,
        BANNED,
        ISLAND_DELETED
    }
}
