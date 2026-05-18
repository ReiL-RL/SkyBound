package me.reil.skybound.core.listener;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.booster.BoosterManager;
import me.reil.skybound.core.config.CoreConfig;
import me.reil.skybound.core.island.IslandManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

/**
 * Manages island flight.
 * Players can fly on their island if they have the flight booster active.
 */
public final class IslandFlyListener implements Listener {

    private final IslandManager islandManager;
    private final BoosterManager boosterManager;
    private final CoreConfig config;

    public IslandFlyListener(IslandManager islandManager, BoosterManager boosterManager, CoreConfig config) {
        this.islandManager = islandManager;
        this.boosterManager = boosterManager;
        this.config = config;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        String worldName = player.getWorld().getName();
        if (!worldName.startsWith(config.getIslandWorldName())) {
            if (player.getAllowFlight() && !player.hasPermission("skybound.fly.everywhere")) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
            return;
        }

        Island island = islandManager.getIslandAt(player.getLocation());
        if (island == null) {
            disableFly(player);
            return;
        }

        // Check if player is a member and has flight booster
        if (island.getMembers().contains(player.getUniqueId())) {
            if (boosterManager.isActive(island, "flight") || player.hasPermission("skybound.fly")) {
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                }
            } else {
                disableFly(player);
            }
        } else {
            disableFly(player);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (!event.getPlayer().getWorld().getName().startsWith(config.getIslandWorldName())) {
            disableFly(player);
        }
    }

    private void disableFly(Player player) {
        if (player.getAllowFlight() && !player.hasPermission("skybound.fly.everywhere")) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }
}
