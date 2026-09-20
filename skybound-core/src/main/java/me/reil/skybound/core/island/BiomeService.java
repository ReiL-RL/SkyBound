package me.reil.skybound.core.island;

import me.reil.skybound.api.island.Island;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles island biome changes.
 * Changes the biome for all blocks within the island radius.
 */
public final class BiomeService {

    private final JavaPlugin plugin;
    private final int columnsPerTick;

    public BiomeService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.columnsPerTick = Math.max(1, plugin.getConfig().getInt("performance.biome-columns-per-tick", 512));
    }

    /**
     * Change the biome of an entire island.
     * @param island the island
     * @param biome the target biome
     * @return true if successful
     */
    public boolean changeBiome(Island island, Biome biome) {
        return changeBiomeBatched(island, biome, null);
    }

    public boolean changeBiomeBatched(Island island, final Biome biome, final Runnable done) {
        Location center = island.getCenter();
        final World world = center.getWorld();
        if (world == null) return false;

        final int radius = island.getRadius();
        final int cx = center.getBlockX();
        final int cz = center.getBlockZ();
        final int minX = cx - radius;
        final int maxX = cx + radius;
        final int minZ = cz - radius;
        final int maxZ = cz + radius;
        final int minY = getMinHeight(world);
        final int maxY = world.getMaxHeight();

        new BukkitRunnable() {
            private int x = minX;
            private int z = minZ;

            @Override
            public void run() {
                int processed = 0;
                while (x <= maxX && processed < columnsPerTick) {
                    for (int y = minY; y < maxY; y++) {
                        world.setBiome(x, y, z, biome);
                    }
                    processed++;
                    z++;
                    if (z > maxZ) {
                        z = minZ;
                        x++;
                    }
                }

                if (x > maxX) {
                    refreshChunks(world, cx, cz, radius);
                    if (done != null) done.run();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        return true;
    }

    private void refreshChunks(World world, int cx, int cz, int radius) {
        int minChunkX = (cx - radius) >> 4;
        int maxChunkX = (cx + radius) >> 4;
        int minChunkZ = (cz - radius) >> 4;
        int maxChunkZ = (cz + radius) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                world.refreshChunk(chunkX, chunkZ);
            }
        }
    }

    private int getMinHeight(World world) {
        try {
            Object value = world.getClass().getMethod("getMinHeight").invoke(world);
            if (value instanceof Integer) {
                return ((Integer) value).intValue();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * Parse a biome name string to Biome enum.
     */
    public Biome parseBiome(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            return Biome.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
