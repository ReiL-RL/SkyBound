package me.reil.skybound.api.event;

import me.reil.skybound.api.island.Island;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a new island is created.
 */
public class IslandCreateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Island island;
    private final String schematicName;
    private boolean cancelled;

    public IslandCreateEvent(Player player, Island island, String schematicName) {
        this.player = player;
        this.island = island;
        this.schematicName = schematicName;
    }

    public Player getPlayer() { return player; }
    public Island getIsland() { return island; }
    public String getSchematicName() { return schematicName; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
