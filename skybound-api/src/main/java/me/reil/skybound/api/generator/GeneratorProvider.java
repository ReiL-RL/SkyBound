package me.reil.skybound.api.generator;

import me.reil.skybound.api.island.Island;
import org.bukkit.Material;

import java.util.Collection;
import java.util.Map;

/**
 * Ore generator system provider.
 * Manages cobblestone generators that produce configurable ores.
 */
public interface GeneratorProvider {

    /**
     * Get all generator tier definitions.
     */
    Collection<GeneratorTier> getTiers();

    /**
     * Get a specific tier by id.
     */
    GeneratorTier getTier(String tierId);

    /**
     * Get the current generator tier for an island.
     */
    GeneratorTier getIslandTier(Island island);

    /**
     * Roll a random material from the island's current generator tier.
     * Used when cobblestone/basalt forms.
     */
    Material rollMaterial(Island island);

    /**
     * Get the ore distribution for an island's current tier.
     * @return map of Material -> chance (0.0 - 1.0)
     */
    Map<Material, Double> getDistribution(Island island);
}
