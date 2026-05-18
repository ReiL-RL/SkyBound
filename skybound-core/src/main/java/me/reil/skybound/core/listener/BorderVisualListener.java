package me.reil.skybound.core.listener;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.config.CoreConfig;
import me.reil.skybound.core.island.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Shows island border particles when players approach the edge.
 */
public final class BorderVisualListener {

    private final JavaPlugin plugin;
    private final IslandManager islandManager;
    private final CoreConfig config;
    private BukkitTask task;

    public BorderVisualListener(JavaPlugin plugin, IslandManager islandManager, CoreConfig config) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.config = config;
    }

    public void start() {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, 20L, 10L); // Every 0.5 seconds
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().getName().startsWith(config.getIslandWorldName())) continue;

            Island island = islandManager.getIslandAt(player.getLocation());
            if (island == null) continue;

            Location center = island.getCenter();
            int radius = island.getRadius();
            Location playerLoc = player.getLocation();

            // Only show border if player is within 5 blocks of edge
            double distX = Math.abs(playerLoc.getX() - center.getX());
            double distZ = Math.abs(playerLoc.getZ() - center.getZ());
            double maxDist = radius - 5;

            if (distX < maxDist && distZ < maxDist) continue;

            // Show particles along the nearest border edge
            showBorderParticles(player, center, radius);
        }
    }

    private void showBorderParticles(Player player, Location center, int radius) {
        double py = player.getLocation().getY();
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int px = player.getLocation().getBlockX();
        int pz = player.getLocation().getBlockZ();

        // Show particles on the closest border wall
        Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 1.0f);

        // North/South walls
        if (Math.abs(pz - (cz - radius)) < 6) {
            for (int x = px - 5; x <= px + 5; x++) {
                player.spawnParticle(Particle.REDSTONE, x + 0.5, py + 1, cz - radius + 0.5, 1, dust);
            }
        }
        if (Math.abs(pz - (cz + radius)) < 6) {
            for (int x = px - 5; x <= px + 5; x++) {
                player.spawnParticle(Particle.REDSTONE, x + 0.5, py + 1, cz + radius + 0.5, 1, dust);
            }
        }
        // East/West walls
        if (Math.abs(px - (cx - radius)) < 6) {
            for (int z = pz - 5; z <= pz + 5; z++) {
                player.spawnParticle(Particle.REDSTONE, cx - radius + 0.5, py + 1, z + 0.5, 1, dust);
            }
        }
        if (Math.abs(px - (cx + radius)) < 6) {
            for (int z = pz - 5; z <= pz + 5; z++) {
                player.spawnParticle(Particle.REDSTONE, cx + radius + 0.5, py + 1, z + 0.5, 1, dust);
            }
        }
    }
}
