package me.reil.skybound.api.event;

import me.reil.skybound.api.island.Island;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when an island levels up.
 */
public class IslandLevelUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Island island;
    private final int oldLevel;
    private final int newLevel;

    public IslandLevelUpEvent(Island island, int oldLevel, int newLevel) {
        this.island = island;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public Island getIsland() { return island; }
    public int getOldLevel() { return oldLevel; }
    public int getNewLevel() { return newLevel; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
