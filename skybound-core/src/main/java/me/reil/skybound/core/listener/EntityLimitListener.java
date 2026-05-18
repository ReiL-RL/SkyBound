package me.reil.skybound.core.listener;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.island.IslandManager;
import me.reil.skybound.core.upgrade.UpgradeManager;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * Enforces entity limits per island based on upgrades.
 */
public final class EntityLimitListener implements Listener {

    private final IslandManager islandManager;
    private final UpgradeManager upgradeManager;

    public EntityLimitListener(IslandManager islandManager, UpgradeManager upgradeManager) {
        this.islandManager = islandManager;
        this.upgradeManager = upgradeManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) return; // Allow natural in void

        Location location = event.getLocation();
        Island island = islandManager.getIslandAt(location);
        if (island == null) return;

        int limit = upgradeManager.getEntityLimit(island);
        int current = countEntitiesOnIsland(island);

        if (current >= limit) {
            event.setCancelled(true);
        }
    }

    private int countEntitiesOnIsland(Island island) {
        int count = 0;
        Location center = island.getCenter();
        if (center.getWorld() == null) return 0;

        for (Entity entity : center.getWorld().getEntities()) {
            if (entity instanceof Player) continue;
            if (!(entity instanceof LivingEntity)) continue;
            if (island.isWithinBounds(entity.getLocation())) {
                count++;
            }
        }
        return count;
    }
}
