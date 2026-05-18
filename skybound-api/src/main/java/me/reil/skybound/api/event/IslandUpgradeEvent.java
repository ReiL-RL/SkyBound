package me.reil.skybound.api.event;

import me.reil.skybound.api.island.Island;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when an island upgrade is purchased.
 */
public class IslandUpgradeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player buyer;
    private final Island island;
    private final String upgradeId;
    private final int newLevel;
    private final double cost;
    private boolean cancelled;

    public IslandUpgradeEvent(Player buyer, Island island, String upgradeId, int newLevel, double cost) {
        this.buyer = buyer;
        this.island = island;
        this.upgradeId = upgradeId;
        this.newLevel = newLevel;
        this.cost = cost;
    }

    public Player getBuyer() { return buyer; }
    public Island getIsland() { return island; }
    public String getUpgradeId() { return upgradeId; }
    public int getNewLevel() { return newLevel; }
    public double getCost() { return cost; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
