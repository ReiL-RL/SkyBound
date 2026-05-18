package me.reil.skybound.core.world;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/**
 * Void world generator for island worlds.
 * Generates completely empty chunks.
 */
public final class VoidWorldGenerator extends ChunkGenerator {

    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        return createChunkData(world);
    }

    @Override
    public boolean canSpawn(World world, int x, int z) {
        return true;
    }
}
