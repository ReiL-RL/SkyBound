package me.reil.skybound.api.event;

import me.reil.skybound.api.island.Island;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when an island is regenerated (reset).
 */
public class IslandRegenEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Island island;
    private boolean cancelled;

    public IslandRegenEvent(Player player, Island island) {
        this.player = player;
        this.island = island;
    }

    public Player getPlayer() { return player; }
    public Island getIsland() { return island; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
