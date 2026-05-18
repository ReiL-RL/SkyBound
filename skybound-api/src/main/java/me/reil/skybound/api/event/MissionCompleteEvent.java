package me.reil.skybound.api.event;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.mission.Mission;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player completes a mission.
 */
public class MissionCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Island island;
    private final Mission mission;

    public MissionCompleteEvent(Player player, Island island, Mission mission) {
        this.player = player;
        this.island = island;
        this.mission = mission;
    }

    public Player getPlayer() { return player; }
    public Island getIsland() { return island; }
    public Mission getMission() { return mission; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
