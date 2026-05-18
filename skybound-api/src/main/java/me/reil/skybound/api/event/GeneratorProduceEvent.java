package me.reil.skybound.api.event;

import me.reil.skybound.api.island.Island;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a generator produces a block.
 * Addons can listen to this to track generator production.
 */
public class GeneratorProduceEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Island island;
    private final Location location;
    private Material material;
    private boolean cancelled;

    public GeneratorProduceEvent(Island island, Location location, Material material) {
        this.island = island;
        this.location = location;
        this.material = material;
    }

    public Island getIsland() { return island; }
    public Location getLocation() { return location; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
