package me.reil.skybound.api.event;

import me.reil.skybound.api.island.Island;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired when an island is being deleted.
 */
public class IslandDeleteEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Island island;
    private final UUID deletedBy;
    private boolean cancelled;

    public IslandDeleteEvent(Island island, UUID deletedBy) {
        this.island = island;
        this.deletedBy = deletedBy;
    }

    public Island getIsland() { return island; }
    public UUID getDeletedBy() { return deletedBy; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
