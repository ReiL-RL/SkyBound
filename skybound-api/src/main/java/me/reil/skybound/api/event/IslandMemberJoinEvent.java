package me.reil.skybound.api.event;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandRole;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired when a player joins an island team.
 */
public class IslandMemberJoinEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Island island;
    private final UUID playerId;
    private final IslandRole role;
    private boolean cancelled;

    public IslandMemberJoinEvent(Island island, UUID playerId, IslandRole role) {
        this.island = island;
        this.playerId = playerId;
        this.role = role;
    }

    public Island getIsland() { return island; }
    public UUID getPlayerId() { return playerId; }
    public IslandRole getRole() { return role; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
