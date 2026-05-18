package me.reil.skybound.core.listener;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.config.CoreConfig;
import me.reil.skybound.core.island.IslandManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Teleports players home when they fall into the void on island worlds.
 */
public final class VoidFallListener implements Listener {

    private final IslandManager islandManager;
    private final CoreConfig config;

    public VoidFallListener(IslandManager islandManager, CoreConfig config) {
        this.islandManager = islandManager;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getLocation().getY() > 0) return;

        String worldName = player.getWorld().getName();
        if (!worldName.startsWith(config.getIslandWorldName())) return;

        // Teleport to island home or spawn
        Island island = islandManager.getPlayerIsland(player.getUniqueId());
        if (island != null) {
            player.teleport(island.getHome());
        } else {
            // Teleport to world spawn
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVoidDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) return;

        Player player = (Player) event.getEntity();
        String worldName = player.getWorld().getName();
        if (!worldName.startsWith(config.getIslandWorldName())) return;

        event.setCancelled(true);
    }
}
