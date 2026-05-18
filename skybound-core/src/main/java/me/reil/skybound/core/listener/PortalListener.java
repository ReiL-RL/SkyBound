package me.reil.skybound.core.listener;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.config.CoreConfig;
import me.reil.skybound.core.island.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Handles nether/end portal transitions for island worlds.
 * Teleports players to the same coordinates in the corresponding dimension world.
 */
public final class PortalListener implements Listener {

    private final IslandManager islandManager;
    private final CoreConfig config;

    public PortalListener(IslandManager islandManager, CoreConfig config) {
        this.islandManager = islandManager;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        String currentWorld = player.getWorld().getName();
        String baseWorld = config.getIslandWorldName();

        if (!currentWorld.startsWith(baseWorld)) return;

        Island island = islandManager.getPlayerIsland(player.getUniqueId());
        if (island == null) {
            event.setCancelled(true);
            return;
        }

        PlayerTeleportEvent.TeleportCause cause = event.getCause();

        if (cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            if (!config.isNetherEnabled()) {
                event.setCancelled(true);
                return;
            }
            if (island.getLevel() < config.getNetherUnlockLevel()) {
                event.setCancelled(true);
                return;
            }

            String targetWorldName;
            if (currentWorld.equals(baseWorld)) {
                targetWorldName = baseWorld + "_nether";
            } else {
                targetWorldName = baseWorld;
            }

            World targetWorld = Bukkit.getWorld(targetWorldName);
            if (targetWorld == null) {
                event.setCancelled(true);
                return;
            }

            Location to = player.getLocation().clone();
            to.setWorld(targetWorld);
            event.setTo(to);

        } else if (cause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            if (!config.isEndEnabled()) {
                event.setCancelled(true);
                return;
            }
            if (island.getLevel() < config.getEndUnlockLevel()) {
                event.setCancelled(true);
                return;
            }

            String targetWorldName;
            if (currentWorld.equals(baseWorld)) {
                targetWorldName = baseWorld + "_the_end";
            } else {
                targetWorldName = baseWorld;
            }

            World targetWorld = Bukkit.getWorld(targetWorldName);
            if (targetWorld == null) {
                event.setCancelled(true);
                return;
            }

            Location to = player.getLocation().clone();
            to.setWorld(targetWorld);
            event.setTo(to);
        }
    }
}
