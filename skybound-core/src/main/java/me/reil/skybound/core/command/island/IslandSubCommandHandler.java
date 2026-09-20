package me.reil.skybound.core.command.island;

import org.bukkit.entity.Player;

public interface IslandSubCommandHandler {

    boolean handle(Player player, String[] args);
}
