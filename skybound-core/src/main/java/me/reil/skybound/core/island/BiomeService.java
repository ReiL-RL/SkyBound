package me.reil.skybound.core.island;

import me.reil.skybound.api.island.Island;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

/**
 * Handles island biome changes.
 * Changes the biome for all blocks within the island radius.
 */
public final class BiomeService {

    /**
     * Change the biome of an entire island.
     * @param island the island
     * @param biome the target biome
     * @return true if successful
     */
    public boolean changeBiome(Island island, Biome biome) {
        Location center = island.getCenter();
        World world = center.getWorld();
        if (world == null) return false;

        int radius = island.getRadius();
        int cx = center.getBlockX();
        int cz = center.getBlockZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                for (int y = 0; y < world.getMaxHeight(); y++) {
                    world.setBiome(x, y, z, biome);
                }
            }
        }

        // Refresh chunks for players
        int minChunkX = (cx - radius) >> 4;
        int maxChunkX = (cx + radius) >> 4;
        int minChunkZ = (cz - radius) >> 4;
        int maxChunkZ = (cz + radius) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                for (Player player : world.getPlayers()) {
                    if (player.getLocation().distanceSquared(center) < (radius + 32) * (radius + 32)) {
                        // Chunk refresh needed - client will see on relog
                    }
                }
            }
        }

        return true;
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
